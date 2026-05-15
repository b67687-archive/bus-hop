# BusHop-SG ProGuard / R8 Rules

# ── Domain models ──
# BusService, BusInfo, etc. are serialized/deserialized via Gson in the data layer
-keep class com.bushop.sg.domain.model.** { *; }

# ── Data layer DTOs ──
# ArrivelahResponse, LtaBusArrivalResponse and nested DTOs use Gson @SerializedName
-keep class com.bushop.sg.data.api.** { *; }

# ── Retrofit interfaces ──
# All Retrofit API interfaces must be kept for dynamic proxy to work
-keep,allowobfuscation interface com.bushop.sg.data.api.ArrivelahApi
-keep,allowobfuscation interface com.bushop.sg.data.api.LtaDataMallApi

# ── Coroutine internals (required for R8 full mode) ──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
