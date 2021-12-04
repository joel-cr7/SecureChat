package com.downloader.securechat.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.bumptech.glide.Glide
import com.downloader.securechat.daos.UserDao
import com.downloader.securechat.databinding.ActivitySignUpBinding
import com.downloader.securechat.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.FileNotFoundException

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private var userProfileImage: Uri? = null
    private lateinit var authenticate: FirebaseAuth
    private lateinit var email: String
    private lateinit var name: String
    private lateinit var phoneNo: String
    private lateinit var password: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authenticate = Firebase.auth

        val user = authenticate.currentUser

        //if user already logged in move to MainActivity directly
        if(user!=null){
            finish()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        setTextChanges()
        setListeners()

    }


    //text watchers
    private fun setTextChanges() {
        binding.inputName.editText?.doOnTextChanged { inputText, _, _, _ ->
            if (inputText != null) {
                if(inputText.trim() != "")
                    binding.inputName.error = null
            }
        }
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
        binding.inputConfirmPassword.editText?.doOnTextChanged { inputText, _, _, _ ->
                binding.inputConfirmPassword.error = null
        }
    }


    private fun setListeners(){
        binding.textSignIn.setOnClickListener{
            onBackPressed()
        }
        
        binding.buttonSignUp.setOnClickListener{
            if(isValidSignUpDetails()){
                isLoading(true)
                signUp()
            }
        }

        //to select image for profile pic
        binding.addImage.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setProfileImage.launch(intent)  //calling the registered activity for selecting profile pic
        }
    }


    private fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun signUp() {
        email = binding.inputEmail.editText?.text.toString().trim()
        password = binding.inputConfirmPassword.editText?.text.toString()
        name = binding.inputName.editText?.text.toString()
        phoneNo = binding.phoneNo.editText?.text.toString()

        //when user enters new email and password, authenticate by firebase email/password authentication
        authenticate.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this){ task ->
            if(task.isSuccessful){
                val firebaseUser = authenticate.currentUser
                updateUi(firebaseUser)
            }else {
                // If sign in fails, display a message to the user.
                Log.w("TAG", "createUserWithEmail:failure", task.exception)
                Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                updateUi(null)
            }
        }

    }


    //registering activity for profile pic selection
    private val setProfileImage: ActivityResultLauncher<Intent> = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
    ) {
        if(it.resultCode == RESULT_OK){
            if(it.data!=null){
                userProfileImage = it.data!!.data   //get the URI of the image selected
                Log.d(this.toString(), "SignupActivity: the URI of image is: "+userProfileImage)
                try {
                    Glide.with(this).load(userProfileImage).into(binding.profilePic)
                }catch (e: FileNotFoundException){
                    e.printStackTrace()
                }
            }
        }
    }


    //checking validation of data entered
    private fun isValidSignUpDetails(): Boolean{

        if(userProfileImage==null){
            showToast("Select Profile Pic!")
            return false
        }

        if(binding.inputName.editText?.text.toString().trim().isEmpty()){
            showToast("Enter name")
            binding.inputName.error = "Enter name"
            return false
        }
        else if(binding.phoneNo.editText?.text.toString().trim().isEmpty()){
            showToast("Enter Phone no.")
            binding.phoneNo.error = "Enter Phone no."
            return false
        }
        else if(binding.phoneNo.editText?.text.toString().trim().length != 10){
            showToast("Enter valid Phone no.")
            binding.phoneNo.error = "Enter valid Phone no."
            return false
        }
        else if(binding.inputEmail.editText?.text.toString().trim().isEmpty()){
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
        else if(binding.inputConfirmPassword.editText?.text.toString().trim().isEmpty()){
            showToast("Confirm your password")
            binding.inputConfirmPassword.error = "Confirm your password"
            return false
        }
        else if(binding.inputPassword.editText?.text.toString() != binding.inputConfirmPassword.editText?.text.toString()){
            showToast("Retype password")
            binding.inputConfirmPassword.error = "Retype password"
            return false
        }
        else{
            return true
        }
    }


    private fun isLoading(loading: Boolean){
        if(loading){
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonSignUp.visibility = View.INVISIBLE
        }
        else{
            binding.progressBar.visibility = View.INVISIBLE
            binding.buttonSignUp.visibility = View.VISIBLE
        }
    }


    private fun updateUi(firebaseUser: FirebaseUser?) {
        if(firebaseUser!=null){
            val storage: FirebaseStorage = FirebaseStorage.getInstance()
            val storageRef: StorageReference = storage.reference.child("profile_pics")

            val childRef: StorageReference = storageRef.child(firebaseUser.uid)

            //insert profile pic into cloud storage and call DAO function to add user into firestore
            userProfileImage?.let {
                childRef.putFile(it).addOnSuccessListener {
                    childRef.downloadUrl.addOnSuccessListener { url->
                        Log.d("image object", "updateUi:${url.toString()}")
                        val user = User(firebaseUser.uid, name, url.toString(), email, phoneNo)
                        Log.d("User", "updateUi: user created successfully!! "+user.displayName+ user.email+ user.imageUrl)
                        val userDAO = UserDao()
                        userDAO.addUser(user)
                        Log.d("added", "updateUi: user added in firestore")
                        startActivity(Intent(this, MainActivity::class.java))   //move to MainActivity
                        finish()
                    }
                }
            }
        }
        else{
            isLoading(false)   //load the activity
            Log.d(this.toString(), "updateUi: firebase user is null")
        }
    }

}