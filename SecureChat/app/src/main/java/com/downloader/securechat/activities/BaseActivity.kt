package com.downloader.securechat.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.downloader.securechat.utilities.CacheStorageManager
import com.downloader.securechat.utilities.Constants
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

open class BaseActivity : AppCompatActivity() {

    private lateinit var documentReference: DocumentReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cacheStorageManager = CacheStorageManager(applicationContext)
        val database = FirebaseFirestore.getInstance()
        documentReference = cacheStorageManager.getStringValue(Constants.KEY_USER_ID)?.let {
            database.collection("Users").document(it)
        }!!
    }

    override fun onPause() {
        super.onPause()
        documentReference.update("Availability", 0)
    }

    override fun onResume() {
        super.onResume()
        documentReference.update("Availability", 1)
    }
}