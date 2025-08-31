package com.wcm.smart_network.okhttp.utils

fun <T> MutableList<T>.removeIfa(predicate: (T) -> Boolean) {
    val removeList = filter(predicate)
    removeAll(removeList.toSet())
}