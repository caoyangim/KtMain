package com.cy.ktmain.dispatcher

object CYDispatchers {
    val Default by lazy {
        DispatcherContext(DefaultDispatcher)
    }
}