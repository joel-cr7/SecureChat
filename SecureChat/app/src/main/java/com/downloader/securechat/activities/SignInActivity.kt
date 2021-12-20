package com.downloader.securechat.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.downloader.securechat.daos.UserDao
import com.downloader.securechat.databinding.ActivitySignInBinding
import com.downloader.securechat.utilities.CacheStorageManager
import com.downloader.securechat.utilities.Constants

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var cacheStorageManager: CacheStorageManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cacheStorageManager = CacheStorageManager(applicationContext)

        if(cacheStorageManager.getBooleanValue(Constants.KEY_IS_SIGNED_IN)){
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }

        setTextChanges()
        setListeners()

    }


    //text watchers
    private fun setTextChanges() {
        binding.inputEmail.editText?.doOnTextChanged { inputText, _, _, _ ->
            if (inputText != null) {
                if(inputText.trim() != "" && Patterns.EMAIL_ADDRESS.matcher(inputText).matches())
                    binding.inputEmail.error = null
            }
        }
        binding.inputPassword.editText?.doOnTextChanged { inputText, _, _, _ ->
            if (inputText != null) {
                if(inputText.trim() != "")
                    binding.inputPassword.error = null
            }
        }
    }


    private fun setListeners(){
        binding.textCreateNewAccount.setOnClickListener{
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.buttonSignIn.setOnClickListener{
            if(isValidSignInDetails()){
                SignIn()
            }
        }
    }


    private fun SignIn(){
        isLoading(true)
        val email = binding.inputEmail.editText?.text.toString().trim()
        val password = binding.inputPassword.editText?.text.toString()

        val userDao = UserDao()
        val querySnapshot = userDao.getUser(email, password)
        querySnapshot.addOnCompleteListener{
            if(it.isSuccessful && it.result!=null && it.result.documents.size > 0){
                val documentSnapshot = it.result.documents[0]
                cacheStorageManager.setBooleanValue(Constants.KEY_IS_SIGNED_IN, true)
                cacheStorageManager.setStringValue(Constants.KEY_USER_ID, documentSnapshot.id)
                documentSnapshot.getString("Name")?.let { it1 -> cacheStorageManager.setStringValue(Constants.KEY_NAME, it1) }
                documentSnapshot.getString("Encrypted Image")?.let { it1 -> cacheStorageManager.setStringValue(Constants.KEY_IMAGE, it1)
                    Log.d(this.toString(), "SignIn: img at signin "+it1)}
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finish()
            }
            else{
                isLoading(false)
                showToast("SignIn Failed!!")
            }
        }
    }


    private fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun isLoading(loading: Boolean){
        if(loading){
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonSignIn.visibility = View.INVISIBLE
        }
        else{
            binding.progressBar.visibility = View.INVISIBLE
            binding.buttonSignIn.visibility = View.VISIBLE
        }
    }


    //checking validation of data entered
    private fun isValidSignInDetails(): Boolean{

        if(binding.inputEmail.editText?.text.toString().trim().isEmpty()){
            showToast("Enter email")
            binding.inputEmail.error = "Enter email"
            return false
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.editText?.text.toString().trim()).matches()){
            showToast("Enter valid email")
            binding.inputEmail.error = "Enter valid email"
            return false
        }
        else if(binding.inputPassword.editText?.text.toString().trim().isEmpty()){
            showToast("Enter password")
            binding.inputPassword.error = "Enter password"
            return false
        }
        else{
            return true
        }
    }

}