package me.eugeniomarletti.redux.internal

import me.eugeniomarletti.redux.Action
import me.eugeniomarletti.redux.Dispatcher
import me.eugeniomarletti.redux.Middleware
import me.eugeniomarletti.redux.Store

internal class MiddlewareStore<State>(
        val store: Store<State>,
        middleware: Array<out Middleware<State>>)
: Store<State> by store {

    private val dispatcher: Dispatcher = run {
        var next: Dispatcher = store::dispatch
        for (i in middleware.indices.reversed()) {
            next = middleware[i](this, next)
        }
        next
    }

    override fun dispatch(action: Action): Any? = dispatcher(action)
}
