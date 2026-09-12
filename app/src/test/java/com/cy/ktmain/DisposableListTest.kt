package com.cy.ktmain

import com.cy.ktmain.coroutine.Disposable
import com.cy.ktmain.coroutine.DisposableList
import com.cy.ktmain.coroutine.remove
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DisposableListTest {
    @Test
    fun removeFromNilReturnsNil() {
        val disposable = TestDisposable()

        val result = DisposableList.Nil.remove(disposable)

        assertSame(DisposableList.Nil, result)
    }

    @Test
    fun removeHeadReturnsTail() {
        val head = TestDisposable()
        val tailDisposable = TestDisposable()
        val tail = DisposableList.Cons(tailDisposable, DisposableList.Nil)
        val list = DisposableList.Cons(head, tail)

        val result = list.remove(head)

        assertSame(tail, result)
    }

    @Test
    fun removeOnlyItemReturnsNil() {
        val disposable = TestDisposable()
        val list = disposableListOf(disposable)

        val result = list.remove(disposable)

        assertSame(DisposableList.Nil, result)
    }

    @Test
    fun removeMiddleKeepsOrderAroundRemovedItem() {
        val first = TestDisposable()
        val middle = TestDisposable()
        val last = TestDisposable()
        val list = disposableListOf(first, middle, last)

        val result = list.remove(middle)

        assertHeads(result, first, last)
    }

    @Test
    fun removeTailKeepsEarlierItemsInOrder() {
        val first = TestDisposable()
        val middle = TestDisposable()
        val tail = TestDisposable()
        val list = disposableListOf(first, middle, tail)

        val result = list.remove(tail)

        assertHeads(result, first, middle)
    }

    @Test
    fun removeMissingDisposableKeepsAllItemsInOrder() {
        val first = TestDisposable()
        val second = TestDisposable()
        val missing = TestDisposable()
        val list = disposableListOf(first, second)

        val result = list.remove(missing)

        assertHeads(result, first, second)
    }

    @Test
    fun removeOnlyFirstMatchingDisposable() {
        val first = TestDisposable()
        val repeated = TestDisposable()
        val last = TestDisposable()
        val list = disposableListOf(first, repeated, last, repeated)

        val result = list.remove(repeated)

        assertHeads(result, first, last, repeated)
    }

    @Test
    fun removeUsesDisposableEquality() {
        val first = TestDisposable()
        val equalDisposable = EqualDisposable(id = 1)
        val last = TestDisposable()
        val list = disposableListOf(first, equalDisposable, last)

        val result = list.remove(EqualDisposable(id = 1))

        assertHeads(result, first, last)
    }

    private fun disposableListOf(vararg disposables: Disposable): DisposableList {
        return disposables.foldRight(DisposableList.Nil as DisposableList) { disposable, tail ->
            DisposableList.Cons(disposable, tail)
        }
    }

    private fun assertHeads(list: DisposableList, vararg expected: Disposable) {
        var current = list
        expected.forEach { disposable ->
            assertTrue(current is DisposableList.Cons)
            current as DisposableList.Cons
            assertSame(disposable, current.head)
            current = current.tail
        }
        assertSame(DisposableList.Nil, current)
    }

    private class TestDisposable : Disposable {
        override fun dispose() = Unit
    }

    private data class EqualDisposable(
        private val id: Int,
    ) : Disposable {
        override fun dispose() = Unit
    }
}
