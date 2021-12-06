package com.downloader.securechat.utilities

import android.content.Context

class CacheStorageManager(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE)

    fun clearCache(){
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    fun setStringValue(key: String, value: String){
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getStringValue(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun setBooleanValue(key: String, value: Boolean){
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getBooleanValue(key: String): Boolean{
        return sharedPreferences.getBoolean(key, false)
    }

}