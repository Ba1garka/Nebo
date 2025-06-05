package com.example.nebo.config

import android.content.Context
import com.example.nebo.model.CreatePostRequest
import com.example.nebo.model.DrawingResponse
import com.example.nebo.model.LoginRequest
import com.example.nebo.model.PostResponse
import com.example.nebo.model.RegisterRequest
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody

import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ResponseBody>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ResponseBody>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<Map<String, Any>>

    companion object {
        fun create( context: Context): ApiService {
            val client = OkHttpClient.Builder()
                .addInterceptor(AddCookiesInterceptor(context))
                .addInterceptor(ReceivedCookiesInterceptor(context))
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()

            return Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8082/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }

    @Multipart
    @POST("drawings/upload")
    suspend fun uploadDrawing(@Part file: MultipartBody.Part): Response<DrawingResponse>

    @GET("drawings/my")
    suspend fun getUserDrawings(): Response<List<DrawingResponse>>

    @POST("drawings/post")
    suspend fun createPost(@Body request: CreatePostRequest): Response<PostResponse>

    @GET("drawings/all")
    suspend fun getAllPosts(): Response<List<PostResponse>>

    @POST("/drawings/like/{postId}")
    suspend fun likePost(@Path("postId") postId: Long): Response<PostResponse>

    @POST("/drawings/unlike/{postId}")
    suspend fun unlikePost(@Path("postId") postId: Long): Response<PostResponse>

    @Multipart
    @POST("auth/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<Map<String, String>>

    @POST("/auth/logout")
    suspend fun logout(): Response<Unit>
}