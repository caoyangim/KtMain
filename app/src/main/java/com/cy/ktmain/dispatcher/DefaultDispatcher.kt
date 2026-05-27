package com.cy.ktmain.dispatcher

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object DefaultDispatcher : Dispatcher {

    private const val groupName = "DefaultDispatcher"
    private val threadGroup = ThreadGroup(groupName)

    private val executor: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() + 1,
        { runnable ->
            Thread(threadGroup, runnable,
                "${groupName}-worker-${threadGroup.activeCount()}").apply {
                isDaemon = true
            }
        }
    )

    override fun dispatch(block: () -> Unit) {
        executor.submit(block)
    }

    fun shutdown() {
        executor.shutdown()
    }
}
