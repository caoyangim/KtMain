package com.cy.ktmain

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.concurrent.thread
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.ContinuationInterceptor.Key
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

suspend fun a()  {
    val time = System.currentTimeMillis()
    cyDealy(5000)
    println("当前耗时：${System.currentTimeMillis() - time}")
}

suspend fun cyDealy(timeMillis: Long){
    if (timeMillis <= 0) return // 立即返回

    // 1. 获取当前协程的 Continuation 并挂起
    return suspendCoroutine<Unit> { continuation ->
        thread {
            Thread.sleep(timeMillis)
            continuation.resume(Unit)
        }
    }
}



fun <R, T> launchCoroutine(receiver: R, block: suspend R.() -> T) {
    block.startCoroutine(receiver, object : Continuation<T> {
        override fun resumeWith(result: Result<T>) {
            println("Coroutine End: $result")
        }

        override val context = EmptyCoroutineContext
    })
}
