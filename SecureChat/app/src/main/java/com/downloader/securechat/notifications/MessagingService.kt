package com.downloader.securechat.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.downloader.securechat.R
import com.downloader.securechat.activities.MainActivity
import com.downloader.securechat.models.User
import com.downloader.securechat.utilities.CacheStorageManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

private const val CHANNEL_ID = "MyChannelId"

//Service to receive notifications
class MessagingService : FirebaseMessagingService() {

    //To set updated token of the user ie. your token
    companion object{
        var sharedPref: CacheStorageManager? = null
        //To get token directly from sharedPreerence and also set it to sharedPreference directly (when we access token 'get' will be called)
        var token:String?
        get(){
            return sharedPref?.getStringValue("fcmToken")
        }
        set(value){
            if (value != null) {
                sharedPref?.setStringValue("fcmToken", value)
            }
        }
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        token = newToken   //to get the new/updated token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val user = User()
        user.id = message.data["userId"].toString()    //these keys ie. 'userId' should be same as Notification Data class as we sent its objects through the notification
        user.displayName = message.data["userName"].toString()
        user.token = message.data["userToken"].toString()

        //main message the notification brings
        val mainMessage = message.data["message"]

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val notificationID = Random.nextInt()
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        //checking for Oreo or higher
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(user.displayName)
            .setContentText(mainMessage)
            .setSmallIcon(R.drawable.ic_notifiation)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mainMessage))
            .build()

        notificationManager.notify(notificationID, notification)  //receive the notification

    }

    //for android Oreo we have to separately create notification channel
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channelName = "notificationChannel"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
            description = "This channel is used for chat notification"
        }
        notificationManager.createNotificationChannel(channel)
    }

}