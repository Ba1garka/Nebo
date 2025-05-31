package com.example.nebo.config

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import android.preference.PreferenceManager
import java.util.HashSet

class ReceivedCookiesInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        val headers = response.headers("Set-Cookie")
        if (headers.isNotEmpty()) {
            val cookies = HashSet<String>()

            headers.forEach { cookie ->
                cookies.add(cookie.split(";".toRegex())[0])
            }

            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet("PREF_COOKIES", cookies)
                .apply()
        }

        return response
    }
}