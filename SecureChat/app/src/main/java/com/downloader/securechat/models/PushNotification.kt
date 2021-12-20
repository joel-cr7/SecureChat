package com.downloader.securechat.models


//note: keep the variables name same as below
data class PushNotification(
    val data: NotificationData,     //this is the data of the notification
    val to: String     //this is the recipient of the notification ie. mention the token here to whom the notification must go
)