@file:Suppress("NOTHING_TO_INLINE")

package me.eugeniomarletti.redux.internal

private object UNDEFINED

@Suppress("UNCHECKED_CAST")
internal inline fun <T> undefined() = UNDEFINED as T

internal inline val Any?.isUndefined: Boolean
    get() = this === UNDEFINED
