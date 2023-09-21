package com.example.ocr_test

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RetrofitService {
    @Headers("Content-Type: application/json")
    @POST("ocr_test.py")
    suspend fun translateText
                (@Body translationRequest: TranslationRequest)
                : Response<String>
//    : Response<TranslationResponse>
}