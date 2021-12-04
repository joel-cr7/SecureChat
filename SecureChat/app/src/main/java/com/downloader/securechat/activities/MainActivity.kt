package com.downloader.securechat.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.downloader.securechat.R
import com.downloader.securechat.daos.UserDao
import com.downloader.securechat.databinding.ActivityMainBinding
import com.downloader.securechat.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authenticate: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authenticate = Firebase.auth
        firebaseUser = authenticate.currentUser!!   //here current user cant be null

        setListeners()

        lifecycleScope.launch(Dispatchers.IO){
            loadUserDetails()
            getToken()
        }
    }


    private fun setListeners(){
        //logout button
        binding.logout.setOnClickListener{

            lifecycleScope.launch(Dispatchers.IO){
                signOut()
                FirebaseAuth.getInstance().signOut()
            }

            //each time the user logout and tries to login again then the gmail chooser must popup
            //so completely log out the user,
            GoogleSignIn.getClient(
                    this,
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        binding.fabNewContact.setOnClickListener{
            startActivity(Intent(this, ContactListActivity::class.java))
        }
    }


    private fun showToast(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }


    private suspend fun loadUserDetails(){
        val userDAO = UserDao()
        val user = userDAO.getUser(firebaseUser.uid).await().toObject(User::class.java)!!
        withContext(Dispatchers.Main){
            binding.name.text = user.displayName
            if(user.imageUrl != ""){
                Glide.with(this@MainActivity).load(user.imageUrl).into(binding.profilePic)
            }
        }
    }


    //get the FCM(firebase cloud messaging) token used for messaging
    private suspend fun getToken(){
        val token = FirebaseMessaging.getInstance().token.await()
        update_FCM_Token(token)
    }


    //add the token to firestore document of the user
    private suspend fun update_FCM_Token(token: String){
        val dbInstance = FirebaseFirestore.getInstance()
        val docRef = dbInstance.collection("users").document(firebaseUser.uid)     //finding firestore document of the current user

        docRef.update("fcmToken", token).await()   //inserting the token
    }


    //delete the token from the firestore document of the user
    private suspend fun signOut(){
        val dbInstance = FirebaseFirestore.getInstance()
        val docRef = dbInstance.collection("users").document(firebaseUser.uid)    //finding firestore document of the current user
        val updates: HashMap<String, Any> = HashMap()
        updates["fcmToken"] = FieldValue.delete()     //The fcmToken from firestore document of the current user will be deleted
        docRef.update(updates as Map<String, Any>).await()
    }

}