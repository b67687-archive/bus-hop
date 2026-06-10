package com.bushop.data.api

import com.google.gson.Gson

/** Shared thread-safe Gson instance — reuse across all data-layer classes. */
object GsonProvider {
    val gson: Gson = Gson()
}
