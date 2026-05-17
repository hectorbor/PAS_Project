package com.mssde.pas_project.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {

    private const val BASE_URL = "https://opendata.aemet.es/"

    // Cliente que acepta todos los certificados (solo para pruebas)
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectionPool(okhttp3.ConnectionPool(0, 1, java.util.concurrent.TimeUnit.NANOSECONDS)) // <- no reutiliza conexiones
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept-Encoding", "identity")
                    .addHeader("Accept", "application/json")
                    .addHeader("Connection", "close") // <- cierra tras cada petición
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val aemetApi: AemetApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getUnsafeOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AemetApi::class.java)
    }

    val aemetDataApi: AemetDataApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://opendata.aemet.es/")
            .client(getUnsafeOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AemetDataApi::class.java)
    }
}