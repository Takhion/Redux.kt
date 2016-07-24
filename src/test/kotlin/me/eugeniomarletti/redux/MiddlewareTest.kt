package me.eugeniomarletti.redux

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiddlewareTest {

    @Test
    fun `init process`() {
        val middleware = CountingMiddleware<Int> { next, action ->
            when (count) {
                1 -> {
                    assertTrue { action === INIT }
                    assertTrue { stateUndefined }
                    assertFails { state }
                }
                2 -> {
                    assertFalse { stateUndefined }
                    assertEquals(0, state)
                }
            }
            next(action)
        }

        assertEquals(0, middleware.count)
        createStoreWithMiddleware(middleware).apply {
            assertEquals(1, middleware.count)
            dispatch(null)
            assertEquals(2, middleware.count)
        }
    }

    @Test
    fun `can change action`() {
        val middleware = middleware<Int> { next, action ->
            next(when (action) {
                is Int -> -action
                else -> action
            })
        }

        createStoreWithMiddleware(middleware).apply {
            assertEquals(0, state)

            val action = 1
            dispatch(action)
            assertEquals(-action, state)
        }
    }

    @Test
    fun `can change dispatch return value`() {
        val result = Object()
        val middleware = middleware<Int> { next, action ->
            next(action)
            result
        }

        createStoreWithMiddleware(middleware).apply {
            assertEquals(result, dispatch(null))
        }
    }

    @Test
    fun `can stop chain`() {
        val middleware = CountingMiddleware<Int> { next, action ->
            // no next
        }

        createStoreWithMiddleware(middleware).apply {
            dispatch(null)
            assertTrue { stateUndefined }
        }

        assertEquals(2, middleware.count)
    }

    @Test
    fun `are called in order`() {
        val middleware2 = CountingMiddleware<Int>()
        val middleware1 = CountingMiddleware<Int> { next, action ->
            assertEquals(count - 1, middleware2.count)
            next(action)
        }

        createStoreWithMiddleware(middleware1, middleware2)
        assertEquals(1, middleware1.count)
    }

    @Test
    fun `can dispatch to the top of the chain`() {
        val middleware1 = CountingMiddleware<Int> { next, action ->
            when (count) {
                2 -> assertEquals(10, action)
                3 -> assertEquals(20, action)
            }
            next(action)
        }
        val middleware2 = CountingMiddleware<Int> { next, action ->
            when (count) {
                2 -> dispatch((action as Int) * 2) // no next, restart chain with new action
                else -> next(action)
            }
        }
        val middleware3 = CountingMiddleware<Int> { next, action ->
            when (count) {
                2 -> assertEquals(20, action)
            }
            next(action)
        }

        createStoreWithMiddleware(middleware1, middleware2, middleware3).apply {
            dispatch(10)
            assertEquals(20, state)
        }
        assertEquals(3, middleware1.count)
        assertEquals(3, middleware2.count)
        assertEquals(2, middleware3.count)
    }

    private fun createStoreWithMiddleware(vararg middleware: Middleware<Int>): Store<Int> {
        val reducer = reducer({ 0 }, { action, state ->
            when (action) {
                is Int -> action + state
                else -> state
            }
        })
        return createStore(reducer, applyMiddleware(*middleware))
    }

    private class CountingMiddleware<in State>(
            private val middleware: CountingDispatcher<State>.(Dispatcher, Action) -> Any?
            = { next, action -> next(action) }
    ) : Middleware<State> {

        val count: Int
            get() = dispatcher?.count ?: 0

        private var dispatcher: CountingDispatcher<State>? = null

        override fun invoke(api: MiddlewareAPI<State>, next: Dispatcher): Dispatcher {
            if (dispatcher != null) throw IllegalStateException("Dispatcher already created")
            return CountingDispatcher(api, next, middleware).apply { dispatcher = this }
        }
    }

    private class CountingDispatcher<out State>(
            api: MiddlewareAPI<State>,
            private val next: Dispatcher,
            private val middleware: CountingDispatcher<State>.(Dispatcher, Action) -> Any?
    ) : Dispatcher, MiddlewareAPI<State> by api {

        var count = 0
            private set

        override fun invoke(action: Action): Any? {
            count++
            return middleware(next, action)
        }
    }
}
