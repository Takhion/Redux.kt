package me.eugeniomarletti.redux

import me.eugeniomarletti.redux.internal.CombinedReducer

/**
 * Helper function to create a [Reducer].
 *
 * @param reduceInitial the function to execute when the state is undefined.
 * @param reduce the normal reducer function.
 * @see reducerDefault
 */
inline fun <State> reducer(
        crossinline reduceInitial: (action: Action) -> State,
        crossinline reduce: (action: Action, state: State) -> State) =
        object : Reducer<State> {
            override fun invoke(action: Action): State = reduceInitial(action)
            override fun invoke(action: Action, state: State): State = reduce(action, state)
        }

/**
 * Helper function to create a [Reducer].
 *
 * @param defaultState provides the default state when it's undefined, which will be passed to the [reduce] function.
 * @param reduce the normal reducer function.
 * @see reducer
 */
inline fun <State> reducerDefault(
        crossinline defaultState: (action: Action) -> State,
        crossinline reduce: (action: Action, state: State) -> State) =
        object : Reducer<State> {
            override fun invoke(action: Action): State = invoke(action, defaultState(action))
            override fun invoke(action: Action, state: State): State = reduce(action, state)
        }

/**
 * Helper function to combine multiple reducers into a single reducer.
 *
 * It will call every child reducer in [reducers], and gather their results into a single [Map] state,
 * whose keys correspond to the keys of the [reducers] map.
 */
fun <K> combineReducers(reducers: Map<K, Reducer<*>>): Reducer<Map<K, *>> = CombinedReducer(reducers)
