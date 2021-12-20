package com.downloader.securechat.utilities

import android.util.Log
import kotlin.math.log

class Constants {
    companion object{
        val KEY_COLLECTION_USERS: String = "Users"
        val KEY_NAME: String = "Name"
        val KEY_EMAIL: String = "email"
        val KEY_PASSWORD: String = "password"
        val KEY_PREFERENCE_NAME: String = "SecureChatAppPreference"
        val KEY_IS_SIGNED_IN: String = "UserIsSignedIn"
        val KEY_USER_ID: String = "UserId"
        val KEY_IMAGE: String = "EncryptedImage"
        val AES_KEY = "EncryptDecryptMessage"
        val KEY_USER: String = "user"
        val KEY_COLLECTION_CHAT = "Chat"
        val KEY_SENDER_ID = "SenderId"
        val KEY_RECEIVER_ID = "ReceiverId"
        val KEY_MESSAGE = "Message"
        val KEY_TIMESTAMP = "TimeStamp"
        val KEY_COLLECTION_CONVERSATIONS = "Conversations"
        val KEY_SENDER_NAME = "SenderName"
        val KEY_RECEIVER_NAME = "ReceiverName"
        val KEY_SENDER_IMAGE = "SenderImage"
        val KEY_RECEIVER_IMAGE = "ReceiverImage"
        val KEY_LAST_MESSAGE = "LastMessage"
        val KEY_AVAILABILITY = "Availability"
        val REMOTE_MSG_AUTHORIZATION = "Authorization"
        val REMOTE_MSG_CONTENT_TYPE = "Content-Type"
        val REMOTE_MSG_DATA = "data"
        val REMOTE_MSG_REGISTRATION_IDS = "registration-ids"

        //for notification
        const val BASE_URL = "https://fcm.googleapis.com"
        const val CONTENT_TYPE = "application/json"
        const val SERVER_KEY = "AAAA4Gn_pvc:APA91bHNqQmVmKnCBh_BvSiJR0YJ8Wnlezk6whhVX-9n36J-QfWgzjjPR-MAeh_H_rkjDRLuHiIHD4daZXeIKySYFmt-TBwlQesIWZ1fxPAlkZrYc7NKJ_q9Gay-F7hyoBSnmnyZt-t4"

        var remoteMsgHeaders: HashMap<String, String>? = null

        fun getMsgHeaders(): HashMap<String, String>{
            if(remoteMsgHeaders==null){
                remoteMsgHeaders = HashMap<String, String>()
                Log.d(this.toString(), "getMsgHeaders: remoteMsgHeaders: "+ remoteMsgHeaders!!)
                remoteMsgHeaders!![REMOTE_MSG_AUTHORIZATION] = "key=AAAA4Gn_pvc:APA91bHNqQmVmKnCBh_BvSiJR0YJ8Wnlezk6whhVX-9n36J-QfWgzjjPR-MAeh_H_rkjDRLuHiIHD4daZXeIKySYFmt-TBwlQesIWZ1fxPAlkZrYc7NKJ_q9Gay-F7hyoBSnmnyZt-t4"
                remoteMsgHeaders!![REMOTE_MSG_CONTENT_TYPE] = "application/json"
            }
            return remoteMsgHeaders as HashMap<String, String>
        }
    }
}