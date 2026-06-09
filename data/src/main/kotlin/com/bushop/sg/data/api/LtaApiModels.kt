package com.bushop.sg.data.api

import com.bushop.sg.domain.model.BusInfo
import com.bushop.sg.domain.model.BusService
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── LTA DataMall API response DTOs ──

data class LtaBusArrivalResponse(
    @SerializedName("Services") val services: List<LtaService>?,
)

data class LtaService(
    @SerializedName("ServiceNo") val serviceNo: String,
    @SerializedName("Operator") val operator: String,
    @SerializedName("NextBus") val nextBus: LtaBusArrival?,
    @SerializedName("NextBus2") val nextBus2: LtaBusArrival?,
    @SerializedName("NextBus3") val nextBus3: LtaBusArrival?,
)

data class LtaBusArrival(
    @SerializedName("EstimatedArrival") val estimatedArrival: String?,
    @SerializedName("Latitude") val latitude: String?,
    @SerializedName("Longitude") val longitude: String?,
    @SerializedName("VisitNumber") val visitNumber: String?,
    @SerializedName("Load") val load: String?,
    @SerializedName("Feature") val feature: String?,
    @SerializedName("Type") val type: String?,
    @SerializedName("OriginCode") val originCode: String?,
    @SerializedName("DestinationCode") val destinationCode: String?,
)

// ── Mappers to domain models ──

fun LtaService.toBusService(): BusService =
    BusService(
        serviceNo = serviceNo,
        operator = operator,
        next = nextBus?.toBusInfo(),
        subsequent = nextBus2?.toBusInfo(),
        next3 = nextBus3?.toBusInfo(),
    )

fun LtaBusArrival.toBusInfo(): BusInfo {
    val (timeText, durationMs) =
        estimatedArrival?.let { parseLtaEstimatedArrival(it) }
            ?: Pair("", 0L)
    return BusInfo(
        time = timeText,
        durationMs = durationMs,
        lat = latitude?.toDoubleOrNull(),
        lng = longitude?.toDoubleOrNull(),
        load = load ?: "",
        feature = feature,
        type = type ?: "",
        visitNumber = visitNumber?.toIntOrNull() ?: 0,
        originCode = originCode,
        destinationCode = destinationCode,
    )
}

/** Parse LTA ISO 8601 estimated arrival like "2024-01-15T10:30:00+08:00" into (displayText, durationMs). */
fun parseLtaEstimatedArrival(isoString: String): Pair<String, Long> {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val arrivalTime = sdf.parse(isoString) ?: return Pair("", 0L)
        val now = Date()
        val diffMs = arrivalTime.time - now.time
        val clampedMs = maxOf(0L, diffMs)
        val timeText = if (clampedMs < 60_000) "Arr." else "${clampedMs / 60_000} min"
        Pair(timeText, clampedMs)
    } catch (e: Exception) {
        Pair("", 0L)
    }
}
