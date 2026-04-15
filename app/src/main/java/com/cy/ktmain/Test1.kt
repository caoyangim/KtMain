package com.cy.ktmain

import android.content.Context
import android.content.pm.PackageManager.GET_SIGNATURES
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume


/*fun main() {
    val suspendFun = suspend {
        println("in continuation...")
        4
    }
    val continuation = suspendFun.createCoroutine(object : Continuation<Int> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Int>) {
            println("end...")
        }
    })
    continuation.resume(Unit)
}*/

object Test1 {
    fun test(context: Context){
context.applicationContext.packageManager.getPackageInfo("",GET_SIGNATURES)
    }
}