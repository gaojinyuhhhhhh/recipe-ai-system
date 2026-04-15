package com.recipe.data.remote

import com.recipe.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 网络客户端单例
 *
 * 提供统一的 HTTP 客户端配置：
 * - 自动附加 JWT Bearer Token（通过 [authInterceptor]）
 * - HTTP 日志输出（Body 级别，调试用）
 * - 60秒超时配置（连接/读/写），兼容AI接口的较长响应时间
 *
 * 使用方式：RetrofitClient.api.xxx()
 */
object RetrofitClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * 认证拦截器：自动附加 Bearer Token
     */
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = TokenManager.getToken()
        android.util.Log.d("RetrofitClient", "Request: ${original.method} ${original.url} | Token: ${if (!token.isNullOrBlank()) "present(${token.length})" else "NULL/EMPTY"}")
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        val response = chain.proceed(request)
        android.util.Log.d("RetrofitClient", "Response: ${response.code} for ${original.url}")
        response
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}