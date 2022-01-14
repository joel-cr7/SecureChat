package com.downloader.securechat.utilities

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class Security {

    companion object{

        private val encryptionKey = byteArrayOf(9,115,51,86,105,4,-31,-23,-68,88,17,20,3,-105,119,-53)
        private val cipher: Cipher = Cipher.getInstance("AES")
        private val deCipher: Cipher = Cipher.getInstance("AES")
        private val secretKeySpec = SecretKeySpec(encryptionKey, "AES")   //to create the secret key

        fun AESEncrypt(plainMessage: String): String{
            val byteString = plainMessage.toByteArray()
            var encodedBytes = ByteArray(byteString.size)
            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
                encodedBytes = cipher.doFinal(byteString)
            }
            catch (e: Exception){
                e.printStackTrace()
            }
            val encodedMessage = String(encodedBytes, charset("ISO-8859-1"))
            return encodedMessage
        }


        fun AESDecrypt(encodedMessage: String): String{
            val encodedBytes = encodedMessage.toByteArray(charset("ISO-8859-1"))
            var decodedBytes = ByteArray(0)
            try {
                deCipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
                decodedBytes = deCipher.doFinal(encodedBytes)
            }
            catch (e: Exception){
                e.printStackTrace()
            }
            val decodedMessage = String(decodedBytes)
            return decodedMessage
        }
    }

}