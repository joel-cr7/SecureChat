package com.downloader.securechat.models

import java.io.Serializable


class User (
        val displayName: String= "",
        val encodedImage: String = "",
        val phone_number: String = "",
        var token: String = "",
        val id: String = "") : Serializable
