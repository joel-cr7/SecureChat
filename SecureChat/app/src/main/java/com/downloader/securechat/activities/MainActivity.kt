package com.downloader.securechat.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.downloader.securechat.daos.UserDao
import com.downloader.securechat.databinding.ActivityMainBinding
import com.downloader.securechat.utilities.CacheStorageManager
import com.downloader.securechat.utilities.Constants
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cacheStorageManager: CacheStorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cacheStorageManager = CacheStorageManager(applicationContext)

        get_FCM_Token()

        loadUserDetails()

        setListeners()

    }


    private fun setListeners(){
        //logout button
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
        binding.name.text = cacheStorageManager.getStringValue(Constants.KEY_NAME)
    }


    private fun decryptProfilePic(encryptedProfilePic: String): Bitmap{
        val bytes = Base64.decode(encryptedProfilePic, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }




    //get FCM(firebase cloud messaging) token used for messaging
    private fun get_FCM_Token(){
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
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


    //delete the token from the firestore document of the user
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

}