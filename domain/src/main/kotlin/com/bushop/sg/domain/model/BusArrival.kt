package com.bushop.sg.domain.model

data class BusArrivalResponse(
    val services: List<BusService>?
)

data class BusService(
    val serviceNo: String,
    val operator: String,
    val next: BusInfo?,
    val subsequent: BusInfo?,
    val next3: BusInfo?
)

data class BusInfo(
    val time: String,
    val durationMs: Long,
    val lat: Double?,
    val lng: Double?,
    val load: String,
    val feature: String?,
    val type: String,
    val visitNumber: Int,
    val originCode: String?,
    val destinationCode: String?
)

fun BusInfo.toDisplayArrival(): DisplayArrival {
    val durationMinutes = durationMs / 60000
    val etaText = if (durationMinutes < 1) "Arr." else "$durationMinutes min"
    return DisplayArrival(
        eta = etaText,
        load = when (load) {
            "SEA" -> "Seats Available"
            "SDA" -> "Standing Available"
            "LSD" -> "Limited Standing"
            else -> load
        },
        isWheelchairAccessible = feature == "WAB",
        busType = when (type) {
            "SD", "Single" -> "Single Decker"
            "DD", "Double" -> "Double Decker"
            "BD", "Bendy" -> "Bendy"
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