package com.downloader.securechat.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.downloader.securechat.Adapters.ChatAdapter
import com.downloader.securechat.daos.ChatDao
import com.downloader.securechat.databinding.ActivityUserChatBinding
import com.downloader.securechat.models.ChatMessage
import com.downloader.securechat.models.User
import com.downloader.securechat.utilities.CacheStorageManager
import com.downloader.securechat.utilities.Constants
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UserChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserChatBinding
    private lateinit var cacheStorageManager: CacheStorageManager
    private lateinit var chatMessages: ArrayList<ChatMessage>
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var database: FirebaseFirestore
    private lateinit var selectedUser: User    //this is the user that is selected from the list of contacts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setListeners()
        loadUserDeails()   //show user details on screen
        initVariables()    //initialize variables (recycler view, adapter, preference manager, etc)
        listenMessagesFromFirestore()   //to listen for changes in firestore and add them to adapter

    }


    private fun setListeners(){
        binding.imageBack.setOnClickListener{
            onBackPressed()
        }

        binding.layoutSend.setOnClickListener{
            val chatDao = ChatDao()
            val senderId = cacheStorageManager.getStringValue(Constants.KEY_USER_ID)!!   //the user should be loggedIn to reach chat activity, so the userId cant be null
            val msg = binding.inputMessage.text.toString()
            chatDao.addMessage(senderId, selectedUser.id, msg, Date())
            binding.inputMessage.text = null
        }
    }


    private fun loadUserDeails() {
        selectedUser = intent.getSerializableExtra("user") as User   //getting the user object
        binding.textName.text = selectedUser.displayName
    }


    private fun initVariables() {
        database = FirebaseFirestore.getInstance()
        cacheStorageManager = CacheStorageManager(applicationContext)
        chatMessages = ArrayList()
        chatAdapter = cacheStorageManager.getStringValue(Constants.KEY_USER_ID)?.let {
            ChatAdapter(chatMessages, getBitmapFromEncryptedString(selectedUser.encodedImage), it)
        }!!    //the user cant be null, so we must get the UserId from preference manager

        binding.chatRecyclerView.adapter = chatAdapter

    }


    private fun listenMessagesFromFirestore(){
        //here we mention the document which is to be listened

        //this is so that you must listen to changes
        database.collection("Chat")
            .whereEqualTo("SenderId", cacheStorageManager.getStringValue(Constants.KEY_USER_ID))
            .whereEqualTo("ReceiverId", selectedUser.id)
            .addSnapshotListener(changeEventListener())   //this eventListener is defined below


        //here sender is the receiver and vice versa
        //this is so that the user with whom you are chatting should also listen for changes
        database.collection("Chat")
            .whereEqualTo("SenderId", selectedUser.id)
            .whereEqualTo("ReceiverId", cacheStorageManager.getStringValue(Constants.KEY_USER_ID))
            .addSnapshotListener(changeEventListener())   //this eventListener is defined below
    }


    //this listens to any changes in the document
    inner class changeEventListener : EventListener<QuerySnapshot> {

        override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
            if (error != null) {
                return
            }
            if (value != null) {
                val count = chatMessages.size
                for (documentChange in value.documentChanges){
                    //checking if any content is added to firestore document
                    if(documentChange.type == DocumentChange.Type.ADDED){
                        val senderId = documentChange.document.getString("SenderId")!!
                        val receiverId = documentChange.document.getString("ReceiverId")!!
                        val message = documentChange.document.getString("Message")!!
                        val dateTime =  getFormattedDateTime(documentChange.document.getDate("TimeStamp")!!)
                        val dateObject = documentChange.document.getDate("TimeStamp")!!

                        //creating the ChatMessage object and adding to adapter
                        val chatMessage = ChatMessage(
                            senderId,
                            receiverId,
                            message,
                            dateTime,
                            dateObject
                        )
                        chatMessages.add(chatMessage)
                    }
                }
                chatMessages.sortWith(Comparator { obj1, obj2 -> obj1.dateObject.compareTo(obj2.dateObject) })
                if(count==0){
                    chatAdapter.notifyDataSetChanged()
                }
                else{
                    chatAdapter.notifyItemRangeInserted(chatMessages.size, chatMessages.size)
                    binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size-1)
                }
                binding.chatRecyclerView.visibility = View.VISIBLE
            }
            binding.progressBar.visibility = View.GONE
        }
    }


    //to get bitmap image from encoded string
    private fun getBitmapFromEncryptedString(encodedProfilePic: String): Bitmap {
        val bytes = Base64.decode(encodedProfilePic, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    private fun getFormattedDateTime(date: Date): String{
        return SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date)
    }

}

