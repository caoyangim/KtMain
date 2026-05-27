package com.cy.ktmain

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

interface Deferred<T> : Job {
    suspend fun await(): T
}

@OptIn(ExperimentalAtomicApi::class)

class DeferredCoroutine<T>(context: CoroutineContext) : AbstractCoroutine<T>(context), Deferred<T> {
    override suspend fun await(): T {
        return when (val currentState = state.load()) {
            is CoroutineState.Complete<*> -> {
                currentState.exception?.let { throw it }
                @Suppress("UNCHECKED_CAST")
                currentState.value as T
            }

            else -> {
                suspendAwait()
            }
        }
    }

    suspend fun suspendAwait(): T = suspendCancellableCoroutine { continuation ->
        val disposable = doOnCompleted { result ->
            continuation.resumeWith(result)
        }
        continuation.invokeOnCancellation { disposable.dispose() }
    }
}

fun <T> async(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T) : Deferred<T> {
    val completion = DeferredCoroutine<T>(context)
    block.startCoroutine(completion)
    return completion
}