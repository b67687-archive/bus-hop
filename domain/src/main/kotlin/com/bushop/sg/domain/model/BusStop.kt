package com.bushop.sg.domain.model

import com.google.gson.annotations.SerializedName

data class BusStop(
    val code: String,
    val name: String = ""
)

data class SavedBusStops(
    @SerializedName("stops") val stops: List<BusStop>
)