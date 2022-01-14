package com.downloader.securechat.models

//All values of a single notification
data class NotificationData(
    var userId: String,
    var userName: String,
    var userToken: String,
    var message: String
)