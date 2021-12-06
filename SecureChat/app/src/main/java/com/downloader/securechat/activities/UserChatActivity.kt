package com.downloader.securechat.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import com.downloader.securechat.Adapters.ChatAdapter
import com.downloader.securechat.daos.ChatDao
import com.downloader.securechat.databinding.ActivityUserChatBinding
import com.downloader.securechat.models.ChatMessage
import com.downloader.securechat.models.User
import com.downloader.securechat.utilities.CacheStorageManager
import com.downloader.securechat.utilities.Constants
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
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

    private fun EventChangeListener(){

    }

    private fun loadUserDeails() {
        selectedUser = intent.getSerializableExtra("user") as User   //getting the user object
        binding.textName.text = selectedUser.displayName
    }

    private fun initVariables() {
        cacheStorageManager = CacheStorageManager(applicationContext)
        chatMessages = ArrayList()
        chatAdapter = cacheStorageManager.getStringValue(Constants.KEY_USER_ID)?.let {
            ChatAdapter(chatMessages, getBitmapFromEncryptedString(selectedUser.encodedImage), it)
        }!!    //the user cant be null, so we must get the UserId from preference manager

        binding.chatRecyclerView.adapter = chatAdapter

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