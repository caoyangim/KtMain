package com.cy.ktmain.dispatcher

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

interface Dispatcher {
    fun dispatch(block: () -> Unit)
}

open class DispatcherContext(private val dispatcher: Dispatcher): AbstractCoroutineContextElement(
    ContinuationInterceptor
), ContinuationInterceptor{
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        DispatcherContinuation(continuation, dispatcher)
}

private class DispatcherContinuation<T>(
    val delegate: Continuation<T>,
    val dispatcher: Dispatcher
): Continuation<T>{
    override val context: CoroutineContext = delegate.context
    override fun resumeWith(result: Result<T>) {
        dispatcher.dispatch {
            delegate.resumeWith(result)
        }
    }
}