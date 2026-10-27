package com.llsl.viper4android.ui.util

import androidx.compose.runtime.Immutable

/**
 * 强跳过（Strong Skipping Mode）友好的不可变集合包装类，
 * 使大集合字段在 Compose 重组中具备稳定的 @Immutable 类型契约。
 */
@Immutable
class ImmutableList<out T>(private val delegate: List<T>) : List<T> by delegate {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImmutableList<*>) return false
        return delegate == other.delegate
    }

    override fun hashCode(): Int = delegate.hashCode()
    override fun toString(): String = delegate.toString()
}

@Immutable
class ImmutableSet<out T>(private val delegate: Set<T>) : Set<T> by delegate {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImmutableSet<*>) return false
        return delegate == other.delegate
    }

    override fun hashCode(): Int = delegate.hashCode()
    override fun toString(): String = delegate.toString()
}

@Immutable
class ImmutableMap<K, out V>(private val delegate: Map<K, V>) : Map<K, V> by delegate {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImmutableMap<*, *>) return false
        return delegate == other.delegate
    }

    override fun hashCode(): Int = delegate.hashCode()
    override fun toString(): String = delegate.toString()
}

private val EmptyImmutableList = ImmutableList<Nothing>(emptyList())
private val EmptyImmutableSet = ImmutableSet<Nothing>(emptySet())
private val EmptyImmutableMap = ImmutableMap<Any?, Nothing>(emptyMap())

fun <T> persistentListOf(): ImmutableList<T> = EmptyImmutableList
fun <T> persistentSetOf(): ImmutableSet<T> = EmptyImmutableSet
@Suppress("UNCHECKED_CAST")
fun <K, V> persistentMapOf(): ImmutableMap<K, V> = EmptyImmutableMap as ImmutableMap<K, V>

fun <T> Iterable<T>.toPersistentList(): ImmutableList<T> =
    if (this is ImmutableList<T>) this else ImmutableList(this.toList())

fun <T> Iterable<T>.toPersistentSet(): ImmutableSet<T> =
    if (this is ImmutableSet<T>) this else ImmutableSet(this.toSet())

fun <K, V> Map<K, V>.toPersistentMap(): ImmutableMap<K, V> =
    if (this is ImmutableMap<K, V>) this else ImmutableMap(this.toMap())
