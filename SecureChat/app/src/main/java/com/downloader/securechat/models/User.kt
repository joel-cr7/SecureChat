package com.downloader.securechat.models

import java.io.Serializable

class User(
        var displayName: String= "",
        var encodedImage: String? = null,
        var phone_number: String = "",
        var token: String = "",
        var id: String = "") : Serializable
