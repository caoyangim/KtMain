package com.cy.ktmain

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

interface Job: CoroutineContext.Element {
    companion object Key: CoroutineContext.Key<Job>
    override val key: CoroutineContext.Key<*> get() = Job

    val isActive: Boolean
    fun invokeOnCancel(onCancel:OnCancel):Disposable
    fun invokeOnCompletion(onCompleted:OnCompleted):Disposable
    fun cancel()
    fun remove(disposable:Disposable)
    suspend fun join()
}

interface Disposable{
    fun dispose()
}
interface OnCancel{
    fun onCancel(cause: Throwable?)
}
typealias OnCompleted = ()-> Unit