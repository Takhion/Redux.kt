package me.eugeniomarletti.redux

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StoreTest {

    @Test
    fun `with reducer`() {
        val reducer = AssertableReducer("foo")
        createStore(reducer)
                .assertInitialised(reducer)
    }

    @Test
    fun `with reducer and initial value`() {
        val initialValue = "bar"
        val reducer = AssertableReducer("foo")
        createStore(reducer, initialValue)
                .assertInitialised(initialValue, reducer)
    }

    @Test
    fun `with reducer and enhancer`() {
        val enhancer: Enhancer<String> = { it }
        val reducer = AssertableReducer("foo")
        createStore(reducer, enhancer)
                .assertInitialised(reducer)
    }

    @Test
    fun `with reducer, initial value and enhancer`() {
        val enhancer: Enhancer<String> = { it }
        val initialValue = "bar"
        val reducer = AssertableReducer("foo")
        createStore(reducer, initialValue, enhancer)
                .assertInitialised(initialValue, reducer)
    }

    @Test
    fun `dispatch`() {
        val reducer = reducer({ action -> "" }, { action, state -> "$state$action" })
        createStore(reducer).apply {
            assertEquals("", state)
            dispatch("foo")
            assertEquals("foo", state)
            dispatch("bar")
            assertEquals("foobar", state)
        }
    }

    @Test
    fun `standard dispatch returns action`() {
        val reducer = reducer({ action -> }, { action, state -> })
        createStore(reducer).apply {
            val action = Object()
            val dispatchResult = dispatch(action)
            assertTrue { dispatchResult === action }
        }
    }

    @Test
    fun `subscription`() {
        val reducer = reducer({ 0 }, { action, state -> if (action == null) state else state + 1 })
        createStore(reducer).apply {
            val listener = CountingListener()
            val subscription = subscribe(listener)
            assertTrue { subscription.subscribed }

            listener.apply {
                assertEquals(0, state)
                assertEquals(0, count)

                dispatch(0)
                assertEquals(1, state)
                assertEquals(1, count)

                dispatch(null)
                assertEquals(1, state)
                assertEquals(1, count)

                subscription.unsubscribe()
                assertFalse { subscription.subscribed }
                dispatch(0)
                assertEquals(2, state)
                assertEquals(1, count)
            }
        }
    }

    @Test
    fun `nested dispatch`() {
        val reducer = reducer({ action -> "" }, { action, state -> "$state$action" })
        createStore(reducer).apply {
            subscribe(CountingListener {
                when (count) {
                    1 -> {
                        dispatch("A")
                        assertEquals(2, count)
                        assertEquals("fooA", state)

                        dispatch("B")
                        assertEquals(3, count)
                        assertEquals("fooAB", state)
                    }
                    2 -> assertEquals("fooA", state)
                    3 -> assertEquals("fooAB", state)
                }
            })

            assertEquals("", state)
            dispatch("foo")
            assertEquals("fooAB", state)
            dispatch("bar")
            assertEquals("fooABbar", state)
        }
    }

    @Test
    fun `adding a listener during dispatch takes effect at the end`() {
        val reducer = reducer({ action -> "" }, { action, state -> "$state$action" })
        createStore(reducer).apply {
            val listener = CountingListener()
            subscribe(CountingListener { if (count == 1) subscribe(listener) })

            dispatch("foo")
            assertEquals(0, listener.count)

            dispatch("bar")
            assertEquals(1, listener.count)
        }
    }

    @Test
    fun `removing a listener during dispatch takes effect at the end`() {
        val reducer = reducer({ action -> "" }, { action, state -> "$state$action" })
        createStore(reducer).apply {
            var subscription: Subscription? = null
            val listener = CountingListener()

            subscribe { subscription!!.unsubscribe() }
            subscription = subscribe(listener)

            dispatch("foo")
            assertEquals(1, listener.count)

            dispatch("bar")
            assertEquals(1, listener.count)
        }
    }

    @Test
    fun `replace reducer`() {
        val reducer1 = reducer({ action -> "1" }, { action, state -> "$state$action" })
        val reducer2 = reducer({ action -> "2" }, { action, state -> "$action$state" })
        createStore(reducer1).apply {
            assertEquals("1", state)
            dispatch("foo")
            dispatch("bar")
            assertEquals("1foobar", state)
            replaceReducer(reducer2)
            assertEquals("${INIT}1foobar", state)
            dispatch("!")
            assertEquals("!${INIT}1foobar", state)
        }
    }

    private class CountingListener(private val action: CountingListener.() -> Unit = { }) : Listener {

        var count = 0
            private set

        override fun invoke() {
            ++count
            action()
        }
    }

    private fun <State> Store<State>.assertInitialised(initialState: State, initDispatched: () -> Boolean) {
        assertTrue { initDispatched() }
        assertFalse { stateUndefined }
        assertEquals(initialState, state)
    }

    private fun <State> Store<State>.assertInitialised(reducer: AssertableReducer<State>)
            = assertInitialised(reducer.initialState, reducer::initDispatched)

    private fun <State> Store<State>.assertInitialised(initialState: State, reducer: AssertableReducer<State>)
            = assertInitialised(initialState, reducer::initDispatched)

    private class AssertableReducer<State>(val initialState: State) : Reducer<State> {

        var initDispatched = false
            private set

        private fun checkInit(action: Action) {
            if (action === INIT) {
                initDispatched = true
            }
        }

        override fun invoke(action: Action): State = initialState.apply { checkInit(action) }
        override fun invoke(action: Action, state: State): State = state.apply { checkInit(action) }
    }
}
