package com.downloader.securechat.models

import java.util.*

class RecentConversationChatMessage (
    val senderId: String = "",
    val receiverId: String = "",
    var message: String = "",
    var dateObject: Date,
    val conversationId: String,
    val conversationName: String,
    val conversationImage: String,
)