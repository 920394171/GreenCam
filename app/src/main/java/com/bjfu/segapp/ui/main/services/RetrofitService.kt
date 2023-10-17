package com.bjfu.segapp.ui.main.services

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface RetrofitService {

    @POST("img")
    @Multipart
    fun uploadOriImg(
        @Part imgFile: MultipartBody.Part,
        @PartMap paramsMap: MutableMap<String, @JvmSuppressWildcards RequestBody>
    ): Call<ResponseBody>

//    @POST("label")
//    @Multipart
//    fun uploadLabel(
//        @Part labelFile: MultipartBody.Part,
//        @PartMap paramsMap: MutableMap<String, @JvmSuppressWildcards RequestBody>
//    ): Call<ResponseBody>

    @GET("get")
    fun getNum(@Query("kw") kw: String): Call<ResponseBody>
}