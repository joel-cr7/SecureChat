package com.downloader.securechat.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import com.downloader.securechat.Adapters.ChatAdapter
import com.downloader.securechat.daos.ChatDao
import com.downloader.securechat.daos.RecentConversationsDao
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

class UserChatActivity : BaseActivity() {

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
        initVariables()    //initialize variables (recycler view, adapter, preference manager, etc)
        listenMessagesFromFirestore()   //to listen for changes in firestore and add them to adapter

    }


    private fun setListeners(){
        binding.imageBack.setOnClickListener{
            onBackPressed()
        }

        binding.layoutSend.setOnClickListener{
            val chatDao = ChatDao()
            val recentConversationsDao = RecentConversationsDao()

            val senderId = cacheStorageManager.getStringValue(Constants.KEY_USER_ID)!!   //the user should be loggedIn to reach chat activity, so the userId cant be null
            val msg = binding.inputMessage.text.toString()
            chatDao.addMessage(senderId, selectedUser.id, msg, Date())


            //this code is to add this user with whom you are chatting on the main chatActivity
            //so this is to create a conversation collection on firestore so that next time this user will appear on main chat activity

            if(conversationId!=null){
                //this means the user with whom you are chatting is already present in conversations collection on firestore,
                //so only update the last message to show it on main chat activity
                recentConversationsDao.updateConversation(binding.inputMessage.text.toString(), conversationId!!)
            }
            else{
                //this means you are chatting with this user first time so add the conversation to firestore
                val docRef = recentConversationsDao.addConversations(cacheStorageManager, selectedUser, binding.inputMessage.text.toString())
                docRef.addOnSuccessListener {
                    conversationId = it.id
                }
            }

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

            //check for any conversations if 'conversationId' is null
            if(conversationId==null){
                checkForConversations()
            }
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


    //checks for any conversations
    private fun checkForConversations(){
        //for a conversation to be between two users the chatMessages must not be null
        if(chatMessages.size != 0){
            checkForConversationsRemotely(cacheStorageManager.getStringValue(Constants.KEY_USER_ID)!!, selectedUser.id)
            checkForConversationsRemotely(selectedUser.id, cacheStorageManager.getStringValue(Constants.KEY_USER_ID)!!)
        }
    }


    //this function is to get conversation collection from firestore so that we can show recent conversations on the main chatActivity
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
            .addSnapshotListener(EventListener{ documentSnapshot: DocumentSnapshot?, firebaseFirestoreException: FirebaseFirestoreException? ->
                if(firebaseFirestoreException!=null){
                    return@EventListener
                }
                if(documentSnapshot!=null){
                    if(documentSnapshot.getLong("Availability")!=null){
                        val availability =
                            Objects.requireNonNull(documentSnapshot.getLong("Availability"))?.toInt()
                        isReceiverAvailable = availability == 1
                    }
                }
                if(isReceiverAvailable){
                    binding.textAvailability.visibility = View.VISIBLE
                }else{
                    binding.textAvailability.visibility = View.GONE
                }
            })
    }


    override fun onResume() {
        super.onResume()
        listenAvailabilityOfReceiver()
    }
}

