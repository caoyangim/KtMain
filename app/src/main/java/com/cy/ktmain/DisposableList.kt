package com.cy.ktmain

sealed class DisposableList {
    object Nil : DisposableList()
    class Cons(
        val head: Disposable, val tail: DisposableList
    ) : DisposableList()
}

fun DisposableList.remove(disposable: Disposable): DisposableList {
    return when (this) {
        DisposableList.Nil -> this
        is DisposableList.Cons -> {
            if (head == disposable) tail
            else DisposableList.Cons(head, tail.remove(disposable))
        }
    }
}

tailrec fun DisposableList.foreach(action: (Disposable) -> Unit): Unit = when (this) {
    DisposableList.Nil -> Unit
    is DisposableList.Cons -> {
        action(head)
        tail.foreach(action)
    }
}

inline fun <reified T : Disposable> DisposableList.loopOn(
    crossinline action: (T) -> Unit
) = foreach {
    when (it) {
        is T -> action(it)
    }
}