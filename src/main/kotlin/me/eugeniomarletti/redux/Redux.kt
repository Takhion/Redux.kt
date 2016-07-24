package me.eugeniomarletti.redux

import me.eugeniomarletti.redux.internal.BaseStore

typealias Action = Any?

/**
 * A Redux store that holds the state tree.
 * The only way to change the data in the store is to call [dispatch] on it.
 *
 * There should only be a single store in your app. To specify how different
 * parts of the state tree respond to actions, you may combine several reducers
 * into a single reducer function by using [combineReducers].
 *
 * @see createStore
 */
interface Store<State> : MiddlewareAPI<State> {
    /**
     * Adds a change listener. It will be called any time an action is dispatched
     * and some part of the state tree may potentially have changed. You may then
     * check [state] to read the current state tree inside the callback.
     *
     * You may call [dispatch] from a change listener, with the following caveats:
     *
     * 1. The subscriptions are snapshotted just before every [dispatch] call.
     * If you subscribe or unsubscribe while the listeners are being invoked, this
     * will not have any effect on the [dispatch] that is currently in progress.
     * However, the next [dispatch] call, whether nested or not, will use a more
     * recent snapshot of the subscription list.
     *
     * 2. The listener should not expect to see all state changes, as the state
     * might have been updated multiple times during a nested [dispatch] before
     * the listener is called. It is, however, guaranteed that all subscribers
     * registered before the [dispatch] started will be called with the latest
     * state by the time it exits.
     *
     * @return a subscription that allows to stop listening.
     */
    fun subscribe(listener: Listener): Subscription

    /**
     * Replaces the current [Reducer] and dispatches a new [INIT] action.
     */
    fun replaceReducer(newReducer: Reducer<State>)
}

/**
 * Subset of [Store] which will be visible from inside a [Middleware].
 */
interface MiddlewareAPI<out State> {
    /**
     * The current state.
     *
     * [Enhancers][Enhancer] might access the state before it's initialised
     * and as such should check [stateUndefined] before reading it.
     */
    val state: State

    /**
     * Returns weather the state is undefined (not initialised).
     *
     * @see INIT
     */
    val stateUndefined: Boolean

    /**
     * Dispatches an action. It is the only way to trigger a state change.
     * The current [Reducer] function will be called with the current state
     * tree and the given [action]. Its return value will be considered the
     * **next** state of the tree, and the change listeners will be notified.
     *
     * @return the [action] itself, unless an [Enhancer] (ie. middleware) overrides it.
     */
    fun dispatch(action: Action): Any?
}

typealias Listener = () -> Unit

interface Subscription {
    val subscribed: Boolean

    /**
     * Stops listening for state changes. If this is called during a dispatch,
     * it will have effect after the dispatch is over.
     */
    fun unsubscribe()
}

/**
 * A pure function that returns the next state, given an action to handle and the current state (if available).
 */
interface Reducer<State> : (Action) -> State, (Action, State) -> State {
    override operator fun invoke(action: Action): State
    override operator fun invoke(action: Action, state: State): State
}

/**
 * A function that enhances (wraps) a store with third-party capabilities such as middleware, time travel,
 * persistence, etc. The only store enhancer that ships with Redux is [applyMiddleware].
 */
typealias Enhancer<State> = (Store<State>) -> Store<State>

private inline fun <State> Store<State>.enhance(enhancer: Enhancer<State>) = enhancer(this)

/**
 * Default [Action] for initialising the state of a [Store].
 *
 * When [createStore] is used, it will be automatically called after the store is created and enhanced,
 * effectively populating the state tree for the first time.
 *
 * [Reducers][Reducer] don't have to know about this, as by contract an unhandled action should return the state,
 * initialising it if necessary.
 */
object INIT {
    override fun toString(): String = "INIT"
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <State> Store<State>.init() = apply { dispatch(INIT) }

/**
 * Creates a Redux [Store] that holds the state tree.
 *
 * @param reducer see [Reducer].
 */
fun <State> createStore(reducer: Reducer<State>)
        = BaseStore(reducer).init()

/**
 * Creates a Redux [Store] that holds the state tree.
 *
 * @param reducer see [Reducer].
 * @param initialState the initial state tree for the store.
 */
fun <State> createStore(reducer: Reducer<State>, initialState: State)
        = BaseStore(reducer, initialState).init()

/**
 * Creates a Redux [Store] that holds the state tree.
 *
 * @param reducer see [Reducer].
 * @param enhancer see [Enhancer].
 */
fun <State> createStore(reducer: Reducer<State>, enhancer: Enhancer<State>)
        = BaseStore(reducer).enhance(enhancer).init()

/**
 * Creates a Redux [Store] that holds the state tree.
 *
 * @param reducer see [Reducer].
 * @param initialState the initial state tree for the store.
 * @param enhancer see [Enhancer].
 */
fun <State> createStore(reducer: Reducer<State>, initialState: State, enhancer: Enhancer<State>)
        = BaseStore(reducer, initialState).enhance(enhancer).init()
