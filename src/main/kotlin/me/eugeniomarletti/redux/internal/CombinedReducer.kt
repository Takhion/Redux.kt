package me.eugeniomarletti.redux.internal

import me.eugeniomarletti.redux.Reducer
import java.util.*

internal class CombinedReducer<K>(private val reducers: Map<K, Reducer<*>>) : Reducer<Map<K, *>> {

    override fun invoke(action: Any?): Map<K, *> {
        return HashMap<K, Any?>(reducers.size).apply {
            for ((name, reducer) in reducers) {
                put(name, reducer(action))
            }
        }
    }

    override fun invoke(action: Any?, state: Map<K, *>): Map<K, *> {
        var newState: MutableMap<K, Any?>? = null
        for ((name, reducer) in reducers) {
            val reducerState = state.getOrElseNullable(name) { undefined() }
            val newReducerState =
                    if (reducerState.isUndefined) {
                        reducer(action)
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        (reducer as Reducer<Any?>)(action, reducerState)
                    }
            if (newReducerState !== reducerState) {
                if (newState == null) {
                    newState = HashMap<K, Any?>(reducers.size).apply { putAll(state) }
                }
                newState[name] = newReducerState
            }
        }
        return newState ?: state
    }

    private inline fun <K, V> Map<K, V>.getOrElseNullable(key: K, defaultValue: () -> V): V {
        val value = get(key)
        if (value == null && !containsKey(key)) {
            return defaultValue()
        } else {
            return value as V
        }
    }
}
