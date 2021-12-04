package com.downloader.securechat.daos

import com.downloader.securechat.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore


class UserDao {

    private val db = FirebaseFirestore.getInstance()
    private val userCollection = db.collection("users")

    fun addUser(user: User){
        user.let {
            userCollection.document(user.userId).set(it)
        }
    }

    fun getUser(userId: String): Task<DocumentSnapshot> {
        return userCollection.document(userId).get()
    }
}