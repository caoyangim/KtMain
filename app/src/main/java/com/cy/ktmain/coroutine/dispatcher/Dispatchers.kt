package com.cy.ktmain.coroutine.dispatcher

object CYDispatchers {
    val Default by lazy {
        DispatcherContext(DefaultDispatcher)
    }
}
