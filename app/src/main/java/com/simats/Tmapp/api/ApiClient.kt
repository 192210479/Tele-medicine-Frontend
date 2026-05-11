package com.simats.Tmapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    const val BASE_URL = "http://103.249.82.251:8002/"

    // Razorpay key — also returned dynamically by /api/payments/create-order
    // This is a fallback in case the server response is empty
    const val RAZORPAY_KEY = ""  // Set your Razorpay test key here: "rzp_test_XXXXXXXX"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val instance: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
