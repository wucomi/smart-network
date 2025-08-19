package com.wcm.smart_network

import android.content.Context
import androidx.core.content.edit

class DiskCacheHostNetwork(context: Context) : IDiskCacheHostNetwork {
    private val sharedPreferences = context.getSharedPreferences("HostNetwork", Context.MODE_PRIVATE)

    override fun getAddressNetwork(address: String): Long {
        return sharedPreferences.getLong(address, 0)
    }

    override fun putAddressNetwork(address: String, networkHandle: Long) {
        sharedPreferences.edit {
            putLong(address, networkHandle)
        }
    }

    override fun removeAddressNetwork(address: String) {
        sharedPreferences.edit {
            remove(address)
        }
    }

    override fun clear() {
        sharedPreferences.edit {
            clear()
        }
    }
}
