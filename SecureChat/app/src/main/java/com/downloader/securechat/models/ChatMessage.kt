package com.downloader.securechat.models

import java.util.*

class ChatMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val dateTime: String = "",
    val dateObject: Date,
)