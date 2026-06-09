package com.bushop.sg.data.api

import com.bushop.sg.domain.api.BusArrivalDataSource
import com.bushop.sg.domain.model.BusArrivalResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Data source backed by LTA DataMall BusArrivalv2 API. */
class LtaBusArrivalDataSource(
    private val apiKey: String,
) : BusArrivalDataSource {
    private val api: LtaDataMallApi by lazy {
        val logging =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    val request =
                        chain
                            .request()
                            .newBuilder()
                            .addHeader("AccountKey", apiKey)
                            .build()
                    chain.proceed(request)
                }.addInterceptor(logging)
                .build()

        Retrofit
            .Builder()
            .baseUrl("https://datamall.lta.gov.sg/ltaodataservice/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LtaDataMallApi::class.java)
    }

    override suspend fun getBusArrivals(busStopCode: String): BusArrivalResponse {
        val ltaResponse = api.getBusArrivals(busStopCode)
        val services = ltaResponse.services?.map { it.toBusService() } ?: emptyList()
        return BusArrivalResponse(services = services)
    }
}
