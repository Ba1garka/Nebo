package com.example.nebo.config


import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import android.preference.PreferenceManager
import java.util.HashSet


class AddCookiesInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()

        val cookies = PreferenceManager.getDefaultSharedPreferences(context)
            .getStringSet("PREF_COOKIES", HashSet()) as HashSet<String>

        if (cookies.isNotEmpty()) {
            builder.header("Cookie", cookies.joinToString("; "))
        }

        return chain.proceed(builder.build())
    }
}