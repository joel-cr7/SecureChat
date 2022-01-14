package com.downloader.securechat.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.downloader.securechat.Adapters.ContactsAdapter
import com.downloader.securechat.databinding.ActivityContactListBinding
import com.downloader.securechat.models.User
import com.downloader.securechat.utilities.CacheStorageManager
import com.google.firebase.firestore.FirebaseFirestore

class ContactListActivity : BaseActivity(), ContactsAdapter.onContactListener {

    private lateinit var binding: ActivityContactListBinding
    val usersContacts: HashMap<String, String> = HashMap()  //users mobile contacts (store name and number in map)
    val userFirebaseContacts: ArrayList<User> = ArrayList()   //users saved contacts which are also in firebase
    private lateinit var cacheStorageManager: CacheStorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cacheStorageManager = CacheStorageManager(applicationContext)

        setListeners()
        checkContactPermission()    //checking if permission is given to access contacts
    }

    private fun setListeners(){
        binding.imageBack.setOnClickListener{
            onBackPressed()
        }
    }

    private fun showErrorMessage(){
        binding.errorMessage.text = String.format("%s", "No contacts available")
        binding.errorMessage.visibility = View.VISIBLE
    }

    private fun loading(isLoading: Boolean){
        if(isLoading){
            binding.progressBar.visibility = View.VISIBLE
        }
        else{
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun checkContactPermission() {
        //if permission is not given
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            //request permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 100)
        }
        else{
            getMobileContactList()
        }
    }

    //get all contacts from user mobile
    @SuppressLint("Range")
    private fun getMobileContactList() {
        loading(true)
        val uri = ContactsContract.Contacts.CONTENT_URI     //initialize URI
        val sort = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC"     // sort in ascending order
        val cursor = contentResolver.query(uri, null, null, null, sort)   //initialize cursor (cursor points to every contact which is filtered using the query)
        if (cursor != null) {
            if(cursor.count>0){
                while (cursor.moveToNext()){
                    val contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))   //get contact ID
                    val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))  // get contact name

                    val phoneURI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI   //initialize phone URI
                    val selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID+" =?"   //mention selection criteria
                    val phoneCursor = contentResolver.query(phoneURI, null, selection, arrayOf(contactId), null)

                    if (phoneCursor != null) {
                        if(phoneCursor.moveToNext()){
                            var phone_no = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            phone_no = phone_no.replace("\\s".toRegex(), "")
                            phone_no = phone_no.replace("+91", "")
                            phone_no = phone_no.trim()
                            usersContacts[name] = phone_no
                        }
                    }
                    phoneCursor?.close()
                }
                cursor.close()
            }
        }
        Log.d(this.toString(), "getContactList: contactlist : $usersContacts")
        getFirebaseContactList(usersContacts)
    }

    //get the users contacts as on 'contactList' page we want to only show user the chats of his saved contacts and not chats of all users of firebase
    //get all contacts from firebase and filter mutual contacts of user and firebase and display them to user
    private fun getFirebaseContactList(usersContacts: HashMap<String, String>) {
        val db = FirebaseFirestore.getInstance()
        val userCollection = db.collection("Users")
        userCollection.get().addOnSuccessListener {
            loading(false)
            //iterate through all the users documents one by one
            Log.d(this.toString(), "getFirebaseContactList: executed "+it.documents)
            for(document in it){
                val user_number = document.getString("Phone No")

                Log.d(this.toString(), "getFirebaseContactList: phoneno "+user_number)

                //iterate through every contact of local storage of user
                for (contact in usersContacts) {
                    val key = contact.key
                    Log.d(this.toString(), "getFirebaseContactList: the key is : "+key+" value is :"+usersContacts[key])

                    //compare phone no. of users contacts and phone no. of firebase if both are same put it in an arraylist and display to user
                    if (usersContacts[key] == user_number) {
                        val user_name = document.getString("Name")
                        val user_image = document.getString("Encrypted Image")
                        Log.d(this.toString(), "getFirebaseContactList: match found "+usersContacts[key])
                        val user_token = document.getString("fcmToken")
                        Log.d(this.toString(), "getFirebaseContactList: token is: "+user_token)
                        if(user_name!=null && user_image!=null && user_number!=null){
                            if(user_token==null){
                                continue
                            }else{
                                userFirebaseContacts.add(User(user_name, user_image, user_number, user_token, document.id))
                            }
                        }
                    }
                }
            }
            if(userFirebaseContacts.size > 0){
                val contactsAdapter = ContactsAdapter(userFirebaseContacts, this)
                binding.contactsRecyclerView.adapter = contactsAdapter
                binding.contactsRecyclerView.visibility = View.VISIBLE
            }
            else{
                showErrorMessage()
            }
        }
        .addOnFailureListener { exception ->
            Toast.makeText(this, "failed to get contacts", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==100 && grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            getMobileContactList()
        }
        else{
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    //interface method to handle clicks on contact
    override fun onContactClicked(user: User) {
        val intent = Intent(applicationContext, UserChatActivity::class.java)
        intent.putExtra("user", user)   //passing the user object with intent
        startActivity(intent)
        finish()
    }

}