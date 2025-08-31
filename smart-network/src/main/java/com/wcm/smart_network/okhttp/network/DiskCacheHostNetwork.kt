package com.wcm.smart_network.okhttp.network

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

internal object DiskCacheHostNetwork : IDiskCacheHostNetwork {
    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("DiskCacheHostNetwork", Context.MODE_PRIVATE)
    }

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
