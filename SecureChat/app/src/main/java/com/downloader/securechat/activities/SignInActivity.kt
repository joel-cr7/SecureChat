package com.downloader.securechat.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.bumptech.glide.Glide
import com.downloader.securechat.R
import com.downloader.securechat.daos.UserDao
import com.downloader.securechat.databinding.ActivitySignInBinding
import com.downloader.securechat.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var authenticate: FirebaseAuth
    private val RC_SIGN_IN: Int = 123
    lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var phoneno: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authenticate = Firebase.auth

        //most of google sign in code is from documentation for firebase google sign in

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setTextChanges()
        setListeners()

    }


    override fun onStart() {
        super.onStart()
        if(authenticate.currentUser!=null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }


    //text watchers
    private fun setTextChanges() {

        binding.phoneNo.editText?.doOnTextChanged { inputText, _, _, _ ->
            if (inputText != null) {
                if(inputText.trim() != "" && inputText.trim().length==10)
                    binding.phoneNo.error = null
            }
        }
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

        binding.googleSigninButton.setOnClickListener{
            if(isValidGoogleSignInDetails()){
                phoneno = binding.phoneNo.editText?.text.toString().trim()
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }
    }


    private fun SignIn(){
        isLoading(true)
        val email = binding.inputEmail.editText?.text.toString().trim()
        val password = binding.inputPassword.editText?.text.toString()
        authenticate.signInWithEmailAndPassword(email, password).addOnCompleteListener{
            if(it.isSuccessful){
                showToast("Login Successful!")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            else{
                isLoading(false)
                showToast("Error in login")
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //add sha1 key to firebase before using any Signin function
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val exception = task.exception

            if(task.isSuccessful){

                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d("sign in Activity", "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w("sign in Activity", "Google sign in failed", e)
                }
            }
            else{
                Log.w("sign in Activity", exception.toString())
                Log.d(this.toString(), "onActivityResult: demo "+exception.toString())
            }
        }
    }


    private fun firebaseAuthWithGoogle(idToken: String) {

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        isLoadingGoogle(true)

        //now authenticate the user
        authenticate.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(this.toString(), "signInWithCredential:success")
                        val user = authenticate.currentUser
                        if(user!=null){
                            showToast("Login Successful!")
                            val googleUser = user.displayName?.let { user.email?.let { it1 -> User(user.uid, it, user.photoUrl.toString(), it1, phoneno) } }
                            if (googleUser != null) {
                                Log.d("User", "updateUi: user created successfully!! "+googleUser.displayName+ googleUser.email+ googleUser.imageUrl)
                                val userDAO = UserDao()
                                userDAO.addUser(googleUser)
                                Log.d("added", "updateUi: user added in firestore")
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            else{
                                Log.d("User", "updateUi: user object is null ")
                            }
                        }
                    } else {
                        // If sign in fails
                        isLoadingGoogle(false)
                        Log.w(this.toString(), "signInWithCredential:failure", task.exception)
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

    private fun isLoadingGoogle(loading: Boolean){
        if(loading){
            binding.googleprogressBar.visibility = View.VISIBLE
            binding.googleSigninButton.visibility = View.INVISIBLE
        }
        else{
            binding.googleprogressBar.visibility = View.INVISIBLE
            binding.googleSigninButton.visibility = View.VISIBLE
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

    //checking validation of phone no. entered
    private fun isValidGoogleSignInDetails(): Boolean {
        if(binding.phoneNo.editText?.text.toString().trim().isEmpty()){
            showToast("Enter Phone No.")
            binding.phoneNo.error = "Enter Phone No."
            return false
        }
        else if(binding.phoneNo.editText?.text.toString().trim().length != 10){
            showToast("Enter valid Phone No.")
            binding.phoneNo.error = "Enter valid Phone No."
            return false
        }
        else{
            return true
        }
    }

}