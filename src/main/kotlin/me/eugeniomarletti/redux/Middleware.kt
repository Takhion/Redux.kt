package me.eugeniomarletti.redux

import me.eugeniomarletti.redux.internal.MiddlewareStore

/**
 * A function that, given a [subset of the store API][MiddlewareAPI] and a function
 * to continue the dispatch to the next element of the chain, returns a [Dispatcher]
 * that can see/handle/interfere with every action dispatched to the store.
 */
typealias Middleware<State> = (api: MiddlewareAPI<State>, next: Dispatcher) -> Dispatcher

/**
 * A function that dispatches an action and returns something.
 */
typealias Dispatcher = (Action) -> Any?

/**
 * Helper function to create a [Middleware].
 */
inline fun <State> middleware(
        crossinline middleware: MiddlewareAPI<State>.(next: Dispatcher, action: Action) -> Any?
): Middleware<State>
        = { api, next -> { action -> middleware(api, next, action) } }

/**
 * Creates a store enhancer that applies [Middleware] to the dispatch method
 * of the Redux store. This is handy for a variety of tasks, such as expressing
 * asynchronous actions in a concise manner, or logging every action payload.
 *
 * Because middleware is potentially asynchronous, this should be the first
 * store enhancer in the composition chain.
 *
 * @param middleware the middleware chain to be applied.
 * @return an [Enhancer] applying the middleware.
 */
fun <State> applyMiddleware(vararg middleware: Middleware<State>): Enhancer<State>
        = { MiddlewareStore(it, middleware) }
