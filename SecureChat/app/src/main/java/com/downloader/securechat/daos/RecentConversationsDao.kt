package com.downloader.securechat.daos

import com.downloader.securechat.models.User
import com.downloader.securechat.utilities.CacheStorageManager
import com.downloader.securechat.utilities.Constants
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.*
import kotlin.collections.HashMap

class RecentConversationsDao {

    private val db = FirebaseFirestore.getInstance()
    private val conversationCollection = db.collection("Conversations")

    //Get conversation collection from firestore to show recent conversations on the chatActivity
    fun checkForConversationsRemotely(senderId: String, receiverId: String): Task<QuerySnapshot> {
        return conversationCollection
            .whereEqualTo("SenderId", senderId)
            .whereEqualTo("ReceiverId", receiverId)
            .get()
    }

    //Add a conversation to firestore
    fun addConversations(cacheStorageManager: CacheStorageManager, selectedUser: User, latestMessage: String): Task<DocumentReference> {
        val conversation: HashMap<String, Any> = HashMap()
        conversation.put("SenderId", cacheStorageManager.getStringValue(Constants.KEY_USER_ID)!!)
        conversation.put("SenderName", cacheStorageManager.getStringValue(Constants.KEY_NAME)!!)
        conversation.put("SenderImage", cacheStorageManager.getStringValue(Constants.KEY_IMAGE)!!)
        conversation.put("ReceiverId", selectedUser.id)
        conversation.put("ReceiverName", selectedUser.displayName)
        selectedUser.encodedImage?.let { conversation.put("ReceiverImage", it) }
        conversation.put("LastMessage", latestMessage)
        conversation.put("TimeStamp", Date())
        return conversationCollection.add(conversation)
    }

    fun updateConversation(latestMessage: String, conversationId: String) {
        val documentReference = conversationCollection.document(conversationId)
        documentReference.update("LastMessage", latestMessage, "TimeStamp", Date())
    }

}