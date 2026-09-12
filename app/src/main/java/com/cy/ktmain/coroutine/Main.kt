package com.cy.ktmain.coroutine

import com.cy.ktmain.coroutine.dispatcher.CYDispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.coroutines.delay

suspend fun main() {
    val job = async(CYDispatchers.Default) {
        delay(1000)
        "123"
    }
    job.invokeOnCompletion {
        println("job completed")
    }
    val str = job.await()
    println(str)
}

fun launch(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> Unit,
): Job {
    val completion = StandCoroutine(context)
    block.startCoroutine(completion)
    return completion
}

class StandCoroutine(context: CoroutineContext) : AbstractCoroutine<Unit>(context)
