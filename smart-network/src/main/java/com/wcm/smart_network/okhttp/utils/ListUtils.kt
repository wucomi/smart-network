package com.hik.smartnetwork.okhttp.utils

fun <T> MutableList<T>.removeIfa(predicate: (T) -> Boolean): Boolean {
    val removeList = filter(predicate)
    if (removeList.isEmpty()) {
        return false
    }
    removeAll(removeList.toSet())
    return true
}