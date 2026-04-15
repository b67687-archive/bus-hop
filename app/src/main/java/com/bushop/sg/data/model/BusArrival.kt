package com.bushop.sg.data.model

import com.google.gson.annotations.SerializedName

data class BusArrivalResponse(
    @SerializedName("services") val services: List<BusService>
)

data class BusService(
    @SerializedName("no") val serviceNo: String,
    @SerializedName("operator") val operator: String,
    @SerializedName("next") val next: BusInfo?,
    @SerializedName("subsequent") val subsequent: BusInfo?,
    @SerializedName("next3") val next3: BusInfo?
)

data class BusInfo(
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

fun BusInfo.toDisplayArrival(): DisplayArrival {
    val durationMinutes = durationMs / 60000
    return DisplayArrival(
        eta = if (durationMinutes < 1) "< 1 min" else "$durationMinutes min",
        load = when (load) {
            "SEA" -> "Seats Available"
            "SDA" -> "Standing Available"
            "LSD" -> "Limited Standing"
            else -> load
        },
        isWheelchairAccessible = feature == "WAB",
        busType = when (type) {
            "SD" -> "Single Decker"
            "DD" -> "Double Decker"
            "BD" -> "Bendy"
            else -> type
        }
    )
}

data class DisplayArrival(
    val eta: String,
    val load: String,
    val isWheelchairAccessible: Boolean,
    val busType: String
)