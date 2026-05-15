package com.bushop.sg.data.api

import com.bushop.sg.domain.model.BusArrivalResponse
import com.bushop.sg.domain.model.BusInfo
import com.bushop.sg.domain.model.BusService
import com.google.gson.annotations.SerializedName

/** Arrivelah API response DTO — keeps Gson annotations in the data layer. */
data class ArrivelahResponse(
    @SerializedName("services") val services: List<ArrivelahService>?
)

data class ArrivelahService(
    @SerializedName("no") val serviceNo: String,
    @SerializedName("operator") val operator: String,
    @SerializedName("next") val next: ArrivelahBusInfo?,
    @SerializedName("subsequent") val subsequent: ArrivelahBusInfo?,
    @SerializedName("next3") val next3: ArrivelahBusInfo?
)

data class ArrivelahBusInfo(
    @SerializedName("time") val time: String,
    @SerializedName("duration_ms") val durationMs: Long,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("load") val load: String,
    @SerializedName("feature") val feature: String?,
    @SerializedName("type") val type: String,
    @SerializedName("visit_number") val visitNumber: Int,
    @SerializedName("origin_code") val originCode: String?,
    @SerializedName("destination_code") val destinationCode: String?
)

fun ArrivelahResponse.toDomain(): BusArrivalResponse = BusArrivalResponse(
    services = services?.map { it.toDomain() }
)

fun ArrivelahService.toDomain(): BusService = BusService(
    serviceNo = serviceNo,
    operator = operator,
    next = next?.toDomain(),
    subsequent = subsequent?.toDomain(),
    next3 = next3?.toDomain()
)

fun ArrivelahBusInfo.toDomain(): BusInfo = BusInfo(
    time = time,
    durationMs = durationMs,
    lat = lat,
    lng = lng,
    load = load,
    feature = feature,
    type = type,
    visitNumber = visitNumber,
    originCode = originCode,
    destinationCode = destinationCode
)
