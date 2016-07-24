@file:Suppress("NOTHING_TO_INLINE")

package me.eugeniomarletti.redux.internal

import me.eugeniomarletti.redux.*
import java.util.*

internal class BaseStore<State>(
        private var reducer: Reducer<State>,
        initialState: State = undefined())
: Store<State> {

    override val state: State
        get() = if (stateUndefined) {
            throw IllegalStateException("State isn't initialised")
        } else {
            stateInternal
        }

    private var stateInternal: State = initialState
        set(newState) {
            if (field !== newState) {
                field = newState
                dispatchToListeners()
            }
        }

    override val stateUndefined: Boolean
        get() = stateInternal.isUndefined

    private var listeners = ArrayList<Listener>()

    private var reducing: Boolean = false

    private var dispatching: Boolean = false

    override fun dispatch(action: Action) = action.apply {
        if (reducing) {
            throw IllegalStateException("Reducers may not dispatch actions")
        }
        stateInternal = reduce(this)
    }

    private inline fun reduce(action: Action): State = try {
        reducing = true
        if (stateUndefined) {
            reducer(action)
        } else {
            reducer(action, state)
        }
    } finally {
        reducing = false
    }

    private inline fun dispatchToListeners() = try {
        dispatching = true
        for (listener in listeners) {
            listener()
        }
    } finally {
        dispatching = false
    }

    override fun replaceReducer(newReducer: Reducer<State>) {
        reducer = newReducer
        init()
    }

    override fun subscribe(listener: Listener): Subscription {
        addListener(listener)
        return SubscriptionImpl(listener)
    }

    private inline fun addListener(noinline listener: Listener) {
        snapshotListeners()
        listeners.add(listener)
    }

    private inline fun removeListener(noinline listener: Listener) {
        snapshotListeners()
        listeners.remove(listener)
    }

    private inline fun snapshotListeners() {
        if (dispatching) {
            dispatching = false
            listeners = ArrayList(listeners)
        }
    }

    inner class SubscriptionImpl(private val listener: Listener) : Subscription {

        override var subscribed: Boolean = true

        override fun unsubscribe() {
            if (subscribed) {
                subscribed = false
                removeListener(listener)
            }
        }
    }
}
