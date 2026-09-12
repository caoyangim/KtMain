package com.cy.ktmain.coroutine

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalAtomicApi::class)
abstract class AbstractCoroutine<T>(context: CoroutineContext) : Job, Continuation<T> {
    protected val state = AtomicReference<CoroutineState>(CoroutineState.Incomplete())
    override val context: CoroutineContext = context + this

    val isCompleted
        get() = state.load() is CoroutineState.Complete<*>

    override val isActive: Boolean
        get() = when (state.load()) {
            is CoroutineState.Complete<*>,
            is CoroutineState.Cancelling -> false

            else -> true
        }

    override fun invokeOnCompletion(onCompleted: OnCompleted): Disposable {
        return doOnCompleted { _ -> onCompleted() }
    }

    protected fun doOnCompleted(block: (Result<T>) -> Unit): Disposable {
        val disposable = CompletionHandler(block, this)
        val newState = state.updateAndFetch { currentState ->
            when (currentState) {
                is CoroutineState.Incomplete -> {
                    currentState.with(disposable)
                }

                is CoroutineState.Cancelling -> {
                    currentState.with(disposable)
                }

                is CoroutineState.Complete<*> -> {
                    currentState
                }
            }
        }
        (newState as? CoroutineState.Complete<T>)?.let {
            block(
                when {
                    it.value != null -> Result.success(it.value)
                    it.exception != null -> Result.failure(it.exception)
                    else -> throw IllegalStateException("Won't happen")
                }
            )
        }
        return disposable
    }

    override fun invokeOnCancel(onCancel: OnCancel): Disposable {
        val handler = CancelHandler(onCancel) { state.load().without(it) }
        state.load().with(handler)
        return handler
    }

    override fun remove(disposable: Disposable) {
        state.load().without(disposable)
    }

    override fun cancel() {
        val cause = CancellationException("Job was cancelled")
        while (true) {
            val current = state.load()
            when (current) {
                is CoroutineState.Incomplete -> {
                    val cancelling = CoroutineState.Cancelling().from(current)
                    if (state.compareAndSet(current, cancelling)) {
                        cancelling.disposableList.loopOn<CancelHandler> { it.onCancel.onCancel(cause) }
                        val completed =
                            CoroutineState.Complete<Nothing>(exception = cause).from(cancelling)
                        state.store(completed)
                        completed.clear()
                        return
                    }
                }

                is CoroutineState.Cancelling, is CoroutineState.Complete<*> -> return
            }
        }
    }

    override suspend fun join() {
        if (isCompleted) return
        suspendCancellableCoroutine<Unit> { continuation ->
            val disposable = invokeOnCompletion {
                continuation.resume(Unit)
            }
            continuation.invokeOnCancellation {
                disposable.dispose()
            }
        }
    }

    override fun resumeWith(result: Result<T>) {
        val newState = state.updateAndFetch { prev ->
            when (prev) {
                is CoroutineState.Cancelling,
                is CoroutineState.Incomplete -> {
                    CoroutineState.Complete(result.getOrNull(), result.exceptionOrNull()).from(prev)
                }

                is CoroutineState.Complete<*> -> {
                    throw IllegalStateException("Already completed")
                }
            }
        }
        newState.notifyCompletion(result)
        newState.clear()
    }
}

private class CancelHandler(
    val onCancel: OnCancel,
    private val onDispose: (Disposable) -> Unit,
) : Disposable {
    override fun dispose() {
        onDispose(this)
    }
}

class CompletionHandler<T>(
    val onCompleted: (Result<T>) -> Unit,
    private val job: Job,
) : Disposable {
    override fun dispose() {
        job.remove(this)
    }
}
