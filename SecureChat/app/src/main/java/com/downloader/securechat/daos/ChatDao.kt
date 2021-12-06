package com.downloader.securechat.daos

import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.collections.HashMap

class ChatDao {

    private val db = FirebaseFirestore.getInstance()
    private val userCollection = db.collection("Chat")

    //add user info ie. (senderId, receiverId, the message sent, timestamp) to database
    fun addMessage(senderId: String, receiverId: String, msg: String, date: Date){
        val message: HashMap<String, Any> = HashMap()
        message["SenderId"] = senderId
        message["ReceiverId"] = receiverId
        message["Message"] = msg
        message["TimeStamp"] = date
        userCollection.add(message)
    }

}