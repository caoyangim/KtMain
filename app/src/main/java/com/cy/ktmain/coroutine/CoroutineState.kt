package com.cy.ktmain.coroutine

sealed class CoroutineState {
    class Incomplete : CoroutineState()
    class Cancelling : CoroutineState()
    class Complete<T>(val value: T? = null, val exception: Throwable? = null) : CoroutineState()

    internal var disposableList: DisposableList = DisposableList.Nil

    fun from(state: CoroutineState): CoroutineState {
        this.disposableList = state.disposableList
        return this
    }

    fun with(disposable: Disposable): CoroutineState {
        this.disposableList = DisposableList.Cons(disposable, this.disposableList)
        return this
    }

    fun without(disposable: Disposable): CoroutineState {
        this.disposableList = this.disposableList.remove(disposable)
        return this
    }

    fun <T> notifyCompletion(result: Result<T>) {
        this.disposableList.loopOn<CompletionHandler<T>> { it.onCompleted(result) }
    }

    fun clear() {
        this.disposableList.foreach { it.dispose() }
        this.disposableList = DisposableList.Nil
    }
}
