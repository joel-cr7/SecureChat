package com.downloader.securechat.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import com.downloader.securechat.Adapters.RecentConversationsAdapter
import com.downloader.securechat.daos.UserDao
import com.downloader.securechat.databinding.ActivityMainBinding
import com.downloader.securechat.models.RecentConversationChatMessage
import com.downloader.securechat.models.User
import com.downloader.securechat.utilities.CacheStorageManager
import com.downloader.securechat.utilities.Constants
import com.downloader.securechat.utilities.Security
import com.google.firebase.firestore.*
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : BaseActivity(), RecentConversationsAdapter.onConversationListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cacheStorageManager: CacheStorageManager
    private lateinit var recentConversations: ArrayList<RecentConversationChatMessage>
    private lateinit var recentConversationsAdapter: RecentConversationsAdapter
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cacheStorageManager = CacheStorageManager(applicationContext)

        initVariables()

        get_FCM_Token()

        loadUserDetails()

        setListeners()

        listenConversationsFromFirestore()

    }

    private fun initVariables(){
        recentConversations = ArrayList()
        recentConversationsAdapter = RecentConversationsAdapter(recentConversations, this)
        binding.conversationsRecyclerView.adapter = recentConversationsAdapter
        database = FirebaseFirestore.getInstance()
    }

    private fun setListeners(){
        binding.logout.setOnClickListener{
            signOut()
        }
        binding.fabNewContact.setOnClickListener{
            startActivity(Intent(this, ContactListActivity::class.java))
        }
    }

    private fun showToast(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun loadUserDetails(){
        val profilePic = cacheStorageManager.getStringValue(Constants.KEY_IMAGE)?.let { decryptProfilePic(it) }
        binding.profilePic.setImageBitmap(profilePic)
        binding.name.text = cacheStorageManager.getStringValue(Constants.KEY_NAME)?.toUpperCase()
    }

    private fun decryptProfilePic(encryptedProfilePic: String): Bitmap{
        val bytes = Base64.decode(encryptedProfilePic, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun listenConversationsFromFirestore(){
        //applying listener to both the users ie. both the users chatting
        database.collection("Conversations")
            .whereEqualTo("SenderId", cacheStorageManager.getStringValue(Constants.KEY_USER_ID))
            .addSnapshotListener(changeEventListener())

        database.collection("Conversations")
            .whereEqualTo("ReceiverId", cacheStorageManager.getStringValue(Constants.KEY_USER_ID))
            .addSnapshotListener(changeEventListener())

    }

    //listen to any changes in the document (show the users that you have recently chatted with on their main chatActivity)
    inner class changeEventListener : EventListener<QuerySnapshot> {

        override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
            if (error != null) {
                return
            }
            if (value != null) {
                for (documentChange in value.documentChanges){

                    //check if any 'conversation' is added to firestore
                    if(documentChange.type == DocumentChange.Type.ADDED){

                        val senderId = documentChange.document.getString("SenderId")!!
                        val receiverId = documentChange.document.getString("ReceiverId")!!

                        if(cacheStorageManager.getStringValue(Constants.KEY_USER_ID).equals(senderId)){

                            //creating the 'recentConversationChatMessage' object and adding to adapter
                            //below 'receiver' is the user with whom you have chatted
                            val receiverProfilePic =  documentChange.document.getString("ReceiverImage")!!
                            val receiverName =  documentChange.document.getString("ReceiverName")!!
                            val conversationId =  documentChange.document.getString("ReceiverId")!!
                            val lastMessage = Security.AESDecrypt(documentChange.document.getString("LastMessage")!!)
                            val dateObject = documentChange.document.getDate("TimeStamp")!!

                            val recentConversationChatMessage = RecentConversationChatMessage(
                                senderId,
                                receiverId,
                                lastMessage,
                                dateObject,
                                conversationId,
                                receiverName,
                                receiverProfilePic
                            )
                            recentConversations.add(recentConversationChatMessage)

                        }
                        else{
                            //create the 'recentConversationChatMessage' object and add to adapter
                            val senderProfilePic =  documentChange.document.getString("SenderImage")!!
                            val senderName =  documentChange.document.getString("SenderName")!!
                            val conversationId =  documentChange.document.getString("SenderId")!!
                            val lastMessage = Security.AESDecrypt(documentChange.document.getString("LastMessage")!!)
                            val dateObject = documentChange.document.getDate("TimeStamp")!!

                            val recentConversationChatMessage = RecentConversationChatMessage(
                                senderId,
                                receiverId,
                                lastMessage,
                                dateObject,
                                conversationId,
                                senderName,
                                senderProfilePic
                            )
                            recentConversations.add(recentConversationChatMessage)
                        }
                    }
                    //check for modifications in 'conversations' document in firestore to show latest message chatActivity
                    else if(documentChange.type == DocumentChange.Type.MODIFIED){
                        for(conversation in recentConversations){
                            val senderId = documentChange.document.getString("SenderId")
                            val receiverId = documentChange.document.getString("ReceiverId")
                            if(conversation.senderId == senderId && conversation.receiverId == receiverId){
                                //update latestMessage and its timestamp
                                conversation.message = Security.AESDecrypt(documentChange.document.getString("LastMessage")!!)
                                conversation.dateObject = documentChange.document.getDate("TimeStamp")!!
                                break
                            }
                        }
                    }
                }

                recentConversations.sortWith { obj1, obj2 -> obj1.dateObject.compareTo(obj2.dateObject) }
                recentConversationsAdapter.notifyDataSetChanged()
                binding.conversationsRecyclerView.visibility = View.VISIBLE
                binding.conversationsRecyclerView.smoothScrollToPosition(0)
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    //get FCM(firebase cloud messaging) token used for messaging
    private fun get_FCM_Token(){
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            cacheStorageManager.setStringValue("fcmToken", token)
            val userDao = UserDao()
            val task = cacheStorageManager.getStringValue(Constants.KEY_USER_ID)?.let { userDao.update_FCM_Token(token, it) }
            Log.d(this.toString(), "getToken: received token: $token")
            if (task != null) {
                task.addOnFailureListener{
                    showToast("Failed Token Update")
                }
            }
        }
    }

    //delete token from the firestore document of the user
    private fun signOut(){
        val userDao = UserDao()
        val task = cacheStorageManager.getStringValue(Constants.KEY_USER_ID)?.let { userDao.delete_FCM_Token(it) }
        if (task != null) {
            task.addOnSuccessListener {
                cacheStorageManager.clearCache()
                showToast("Signed out")
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
            }
            .addOnFailureListener{
                showToast("Failed to Sign out")
            }
        }
    }

    override fun onConversationClicked(user: User) {
        val intent = Intent(applicationContext, UserChatActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent)
    }

}