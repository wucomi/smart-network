package com.hik.network.proxy.utils

fun <T> MutableList<T>.removeIfa(predicate: (T) -> Boolean) {
    val removeList = filter(predicate)
    removeAll(removeList.toSet())
}