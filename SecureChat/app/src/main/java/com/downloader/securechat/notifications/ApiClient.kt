package com.downloader.securechat.notifications

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

//created for notification purpose
class ApiClient {

//    private var retrofit: Retrofit? = null

    fun getClient(): Retrofit {
//        if(retrofit == null){
//        retrofit =
            return  Retrofit.Builder().baseUrl("https://fcm.googleapis.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
//        }
//        return retrofit
    }


//    val api: ApiService by lazy {
//         Retrofit.Builder()
//             .baseUrl("https://fcm.googleapis.com/fcm/")
//             .addConverterFactory(ScalarsConverterFactory.create())
//             .build()
//             .create(ApiService::class.java)
//    }


}