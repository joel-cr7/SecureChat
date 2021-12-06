package com.downloader.securechat.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.downloader.securechat.daos.UserDao
import com.downloader.securechat.databinding.ActivitySignUpBinding
import com.downloader.securechat.utilities.CacheStorageManager
import com.downloader.securechat.utilities.Constants
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.util.Collections.rotate


class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private var encodedImage: String? = null
    private lateinit var authenticate: FirebaseAuth
    private lateinit var email: String
    private lateinit var name: String
    private lateinit var phoneNo: String
    private lateinit var password: String
    private lateinit var cacheStorageManager: CacheStorageManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cacheStorageManager = CacheStorageManager(applicationContext)

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


    private fun encryptImage(bitmap: Bitmap): String{
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
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

        val userDAO = UserDao()
        val addUserTask = userDAO.addUser(name, email, password, encodedImage, phoneNo)
        addUserTask.addOnSuccessListener {
            isLoading(false)
            cacheStorageManager.setBooleanValue(Constants.KEY_IS_SIGNED_IN, true)
            cacheStorageManager.setStringValue(Constants.KEY_USER_ID, it.id)
            cacheStorageManager.setStringValue(Constants.KEY_NAME, name)
            encodedImage?.let { it1 -> cacheStorageManager.setStringValue(Constants.KEY_IMAGE, it1) }
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }
        .addOnFailureListener{
            isLoading(false)
            showToast("Failed Sign Up!!")
        }
    }

    //registering activity for profile pic selection
    private val setProfileImage: ActivityResultLauncher<Intent> = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
    ) {
        if(it.resultCode == RESULT_OK){
            if(it.data!=null){
                val userProfileImageUri = it.data!!.data   //get the URI of the image selected
                try {
                    val inputStream = userProfileImageUri?.let { it1 -> contentResolver.openInputStream(it1) }
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    binding.profilePic.setImageBitmap(bitmap)
                    encodedImage = encryptImage(bitmap)
                }catch (e: FileNotFoundException){
                    e.printStackTrace()
                }
            }
        }
    }


    //checking validation of data entered
    private fun isValidSignUpDetails(): Boolean{

        if(encodedImage==null){
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

}