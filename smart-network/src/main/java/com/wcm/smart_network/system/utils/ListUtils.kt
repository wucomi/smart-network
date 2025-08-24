package com.wcm.smart_network.system.utils

fun <T> MutableList<T>.removeIfa(predicate: (T) -> Boolean) {
    val removeList = filter(predicate)
    removeAll(removeList.toSet())
}