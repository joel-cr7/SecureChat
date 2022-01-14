package com.downloader.securechat.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.View
import com.downloader.securechat.Adapters.ChatAdapter
import com.downloader.securechat.daos.ChatDao
import com.downloader.securechat.daos.RecentConversationsDao
import com.downloader.securechat.databinding.ActivityUserChatBinding
import com.downloader.securechat.models.ChatMessage
import com.downloader.securechat.models.NotificationData
import com.downloader.securechat.models.PushNotification
import com.downloader.securechat.models.User
import com.downloader.securechat.notifications.MessagingService
import com.downloader.securechat.notifications.RetrofitInstance
import com.downloader.securechat.utilities.CacheStorageManager
import com.downloader.securechat.utilities.Constants
import com.downloader.securechat.utilities.Security
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UserChatActivity : BaseActivity() {

    private val TAG = "UserChatActivity"
    private lateinit var binding: ActivityUserChatBinding
    private lateinit var cacheStorageManager: CacheStorageManager
    private lateinit var chatMessages: ArrayList<ChatMessage>
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var database: FirebaseFirestore
    private lateinit var selectedUser: User    //this is the user that is selected from the list of contacts
    private var conversationId: String? = null
    private var isReceiverAvailable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setListeners()
        loadUserDeails()   //show user details on screen
        initVariables()    //initialize variables
        listenMessagesFromFirestore()   //listen for changes in firestore and add them to adapter
    }

    private fun setListeners(){
        binding.imageBack.setOnClickListener{
            onBackPressed()
        }
        binding.layoutSend.setOnClickListener {
            if (!TextUtils.isEmpty(binding.inputMessage.text.toString())) {
                val chatDao = ChatDao()
                val recentConversationsDao = RecentConversationsDao()
                val senderId = cacheStorageManager.getStringValue(Constants.KEY_USER_ID)!!
                val msg =
                    Security.AESEncrypt(binding.inputMessage.text.toString())   //encrypt message using created method
                chatDao.addMessage(senderId, selectedUser.id, msg, Date())

                //Add the user with whom you are chatting on the chatActivity
                //Create a conversation collection on firestore so this user will appear on chat activity

                if (conversationId != null) {
                    //it means the user with whom you are chatting is already present in conversations collection on firestore,
                    //only update the last message to show on chat activity
                    recentConversationsDao.updateConversation(
                        Security.AESEncrypt(binding.inputMessage.text.toString()),
                        conversationId!!
                    )
                } else {
                    //Add the conversation to firestore
                    val docRef = recentConversationsDao.addConversations(
                        cacheStorageManager,
                        selectedUser,
                        Security.AESEncrypt(binding.inputMessage.text.toString())
                    )
                    docRef.addOnSuccessListener {
                        conversationId = it.id
                    }
                }

                //this is notification part (if user is offline only then he will receive notification)
                if (!isReceiverAvailable) {
                    val receiverToken =
                        selectedUser.token    //token of the user who will receive the notification
                    MessagingService.sharedPref =
                        cacheStorageManager   //initialize 'sharedPref' of 'MessagingService' as we would get the new/updated token if any
                    MessagingService.token =
                        cacheStorageManager.getStringValue("fcmToken")   //setting user token ie. your token

                    //signed in users info (ie. you)
                    val userId = cacheStorageManager.getStringValue(Constants.KEY_USER_ID)
                    val userName = cacheStorageManager.getStringValue(Constants.KEY_NAME)
                    val userToken = cacheStorageManager.getStringValue("fcmToken")

                    val message = binding.inputMessage.text.toString()
                    Log.d(
                        this.toString(),
                        "setListeners: notification user information is: $userId $userName $userToken $message"
                    )
                    Log.d(
                        this.toString(),
                        "setListeners: notification receiver token is: $receiverToken"
                    )

                    if (receiverToken != null && userId != null && userName != null && userToken != null && message.isNotEmpty()) {
                        //create the 'PushNotification' object and pass it to 'sendNotification'
                        PushNotification(
                            NotificationData(userId, userName, userToken, message),
                            receiverToken
                        ).also {
                            sendNotification(it)
                        }
                    }
                }
                binding.inputMessage.text = null
            }
        }
    }

    private fun loadUserDeails() {
        selectedUser = intent.getSerializableExtra("user") as User   //get user object
        binding.textName.text = selectedUser.displayName
    }

    private fun initVariables() {
        database = FirebaseFirestore.getInstance()
        cacheStorageManager = CacheStorageManager(applicationContext)
        chatMessages = ArrayList()
        Log.d(TAG, "initVariables: reached here")
        chatAdapter = cacheStorageManager.getStringValue(Constants.KEY_USER_ID)?.let {
            Log.d(TAG, "initVariables: got the id")
            selectedUser.encodedImage?.let { it1 ->
                Log.d(TAG, "initVariables: got the image")
                getBitmapFromEncryptedString(it1)?.let { it1 ->
                    Log.d(TAG, "initVariables: set to adapter")
                    ChatAdapter(chatMessages, it1, it)
                }
            }
        }!!
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun listenMessagesFromFirestore(){
        //listen to changes
        database.collection("Chat")
            .whereEqualTo("SenderId", cacheStorageManager.getStringValue(Constants.KEY_USER_ID))
            .whereEqualTo("ReceiverId", selectedUser.id)
            .addSnapshotListener(changeEventListener())   //this eventListener is defined below

        //here sender is the receiver and vice versa
        //The user with whom you are chatting should also listen for changes
        database.collection("Chat")
            .whereEqualTo("SenderId", selectedUser.id)
            .whereEqualTo("ReceiverId", cacheStorageManager.getStringValue(Constants.KEY_USER_ID))
            .addSnapshotListener(changeEventListener())   //this eventListener is defined below
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch{

        //making Post Api call for notification
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful){
                //nothing to do
            }
            else{
                Log.e(TAG, response.errorBody().toString())
            }
        }
        catch(e: Exception){
            Log.e(TAG, e.toString())
        }
    }

    //listen to any changes in the document
    inner class changeEventListener : EventListener<QuerySnapshot> {
        override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
            if (error != null) {
                return
            }
            if (value != null) {
                val count = chatMessages.size
                for (documentChange in value.documentChanges){
                    //check if any content is added to firestore document
                    if(documentChange.type == DocumentChange.Type.ADDED){
                        val senderId = documentChange.document.getString("SenderId")!!
                        val receiverId = documentChange.document.getString("ReceiverId")!!
                        val message = Security.AESDecrypt(documentChange.document.getString("Message")!!)
                        val dateTime =  getFormattedDateTime(documentChange.document.getDate("TimeStamp")!!)
                        val dateObject = documentChange.document.getDate("TimeStamp")!!

                        //create the ChatMessage object and add to adapter
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

            //check for any conversations if 'conversationId' is null
            if(conversationId==null){
                checkForConversations()
            }
        }
    }

    //get bitmap image from encoded string
    private fun getBitmapFromEncryptedString(encodedProfilePic: String?): Bitmap? {
        if(encodedProfilePic!=null){
            val bytes = Base64.decode(encodedProfilePic, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }else{
            return null
        }
    }

    private fun getFormattedDateTime(date: Date): String{
        return SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date)
    }

    //check for any conversations
    private fun checkForConversations(){
        //for a conversation to be between two users the chatMessages must not be null
        if(chatMessages.size != 0){
            checkForConversationsRemotely(cacheStorageManager.getStringValue(Constants.KEY_USER_ID)!!, selectedUser.id)
            checkForConversationsRemotely(selectedUser.id, cacheStorageManager.getStringValue(Constants.KEY_USER_ID)!!)
        }
    }

    //Get conversation collection from firestore to show recent conversations on chatActivity
    private fun checkForConversationsRemotely(senderId: String, receiverId: String) {
        val recentConversationsDao = RecentConversationsDao()
        val task = recentConversationsDao.checkForConversationsRemotely(senderId, receiverId)
        task.addOnCompleteListener{
            if(it.isSuccessful && it.result !=null && it.result.documents.size > 0){
                //if any conversation is found then set 'conversationId' as that document Id
                val documentSnapshot = it.result.documents[0]
                conversationId = documentSnapshot.id
            }
        }
    }

    //check availability of receiver ie. if online or offline
    private fun listenAvailabilityOfReceiver(){
        database.collection("Users").document(selectedUser.id)
            .addSnapshotListener(EventListener { documentSnapshot: DocumentSnapshot?, firebaseFirestoreException: FirebaseFirestoreException? ->
                if (firebaseFirestoreException != null) {
                    return@EventListener
                }
                if (documentSnapshot != null) {
                    if (documentSnapshot.getLong("Availability") != null) {
                        val availability =
                            Objects.requireNonNull(documentSnapshot.getLong("Availability"))
                                ?.toInt()
                        isReceiverAvailable = availability == 1
                    }
                    selectedUser.token = documentSnapshot.getString("fcmToken").toString()
                    if (selectedUser.encodedImage == null) {
                        selectedUser.encodedImage =
                            documentSnapshot.getString("Encrypted Image").toString()
                        Log.d(TAG, "listenAvailabilityOfReceiver: got image: "+selectedUser.encodedImage)
                        chatAdapter.setReceiverProfilePic(getBitmapFromEncryptedString(selectedUser.encodedImage))
                        chatAdapter.notifyItemRangeChanged(0, chatMessages.size)
                    }
                }
                if (isReceiverAvailable) {
                    binding.textAvailability.visibility = View.VISIBLE
                } else {
                    binding.textAvailability.visibility = View.GONE
                }
            })
    }

    override fun onResume() {
        super.onResume()
        listenAvailabilityOfReceiver()
    }
}

