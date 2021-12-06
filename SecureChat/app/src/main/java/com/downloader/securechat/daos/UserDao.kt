package com.downloader.securechat.daos

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*


class UserDao {

    private val db = FirebaseFirestore.getInstance()
    private val userCollection = db.collection("Users")


    //add user info to database
    fun addUser(name:String, email: String, password: String, encryptedImage: String?, phoneNo: String): Task<DocumentReference> {
        val hashMap: HashMap<String, String> = HashMap()
        hashMap["Name"] = name
        hashMap["Email"] = email
        hashMap["Password"] = password
        encryptedImage?.let { hashMap.put("Encrypted Image", it) }
        hashMap["Phone No"] = phoneNo
        return userCollection.add(hashMap)
    }


    fun getUser(email: String, password: String): Task<QuerySnapshot> {
        return userCollection.whereEqualTo("Email", email)
                .whereEqualTo("Password", password)
                .get()
    }


    // add the token to firestore document of the user
    fun update_FCM_Token(token: String, docID: String): Task<Void> {
        val docRef = userCollection.document(docID)    //finding firestore document of the current user

        return docRef.update("fcmToken", token)   //inserting the token and return the task
    }


    //delete the token from the firestore document of the user
    fun delete_FCM_Token(docID: String): Task<Void> {
        val docRef = userCollection.document(docID)
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["fcmToken"] = FieldValue.delete()
        return docRef.update(hashMap)
    }

}