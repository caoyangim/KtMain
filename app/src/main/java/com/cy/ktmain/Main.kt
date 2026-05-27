package com.cy.ktmain

import com.cy.ktmain.dispatcher.CYDispatchers
import com.cy.ktmain.dispatcher.Dispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

suspend fun main() {
    val job = async(CYDispatchers.Default) {
        delay(1000)
        "123"
    }
    val complete = job.invokeOnCompletion {
        println("job completed")
    }
    val str = job.await()
    println(str)
}
fun launch(
    context: CoroutineContext = EmptyCoroutineContext,
    block:suspend ()->Unit
): Job {
    val completion = StandCoroutine(context)
    block.startCoroutine(completion)
    return completion
}

class StandCoroutine(context: CoroutineContext): AbstractCoroutine<Unit>(context) {

}