package me.eugeniomarletti.redux

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlin.test.fail

class ReducerTest {

    @Test
    fun `create`() {
        reducer({ action -> }, { action, state -> })
        reducerDefault({ action -> }, { action, state -> })
    }

    @Test
    fun `default state is used`() {
        val default = Object()
        val state = reducerDefault({ default }, { action, state -> state })(null)
        assertEquals(default, state)
    }

    @Test
    fun `default state is lazy`() {
        reducerDefault<Any?>({ fail() }, { action, state -> })(null, null)
    }

    @Test
    fun `multiple functions`() {
        val reducer = reducer({ action -> 1 }, { action, state -> 2 })

        val oneParam: (Action) -> Int = reducer
        val twoParams: (Action, Int) -> Int = reducer

        assertEquals(1, oneParam(0))
        assertEquals(2, twoParams(0, 0))
    }

    @Test
    fun `combine`() {
        val history = Object()
        val nullableSum = Object()

        val combinedReducer = combineReducers(mapOf(
                history to reducer(
                        { action -> "$action" },
                        { action, state -> "$state -> $action" }),

                nullableSum to reducer<Int?>(
                        { action -> null },
                        { action, state ->
                            when (action) {
                                is Int -> action + (state ?: 0)
                                else -> state
                            }
                        })
        ))

        val state = combinedReducer(0).apply {
            assertEquals("0", get(history))
            assertEquals(null, get(nullableSum))
            assertTrue { containsKey(nullableSum) }
        }

        combinedReducer(10, state).apply {
            assertEquals("0 -> 10", get(history))
            assertEquals(10, get(nullableSum))
        }

        val partialState = HashMap(state).apply { remove(history) }
        combinedReducer(10, partialState).apply {
            assertEquals("10", get(history))
            assertEquals(10, get(nullableSum))
        }

        val wrongState = HashMap(state).apply { put(nullableSum, "string") }
        assertFails { combinedReducer(10, wrongState) }
    }
}
