package com.downloader.securechat.models

//mention all values of a single notification
data class NotificationData(
    var userId: String,
    var userName: String,
    var userToken: String,
    var message: String
)