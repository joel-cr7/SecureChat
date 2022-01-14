package com.downloader.securechat.models

//note: keep the variables name same as below
data class PushNotification(
    val data: NotificationData,     //data of the notification
    val to: String     //recipient of the notification ie. the token to whom the notification must go
)