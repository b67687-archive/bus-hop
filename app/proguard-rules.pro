# BusHop-SG ProGuard / R8 Rules

# ── Domain models ──
# BusService, BusInfo, etc. are serialized/deserialized via Gson in the data layer
-keep class com.bushop.sg.domain.model.** { *; }

# ── Data layer DTOs ──
# ArrivelahResponse DTOs use Gson @SerializedName
-keep class com.bushop.sg.data.api.** { *; }

# BusStopEntry and other local model classes used in Gson operations
-keep class com.bushop.sg.data.local.** { *; }

# ── Retrofit interfaces ──
# All Retrofit API interfaces must be kept for dynamic proxy to work
-keep,allowobfuscation interface com.bushop.sg.data.api.ArrivelahApi

# ── ViewModel ──
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keep class com.bushop.sg.ui.screens.MainViewModel { *; }
-keep class com.bushop.sg.ui.screens.MainViewModel$Factory { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# ── Enum classes (ApiStatus, ThemeMode, ColorSchemeOption) ──
-keepclassmembers enum * { *; }

# ── Gson reflection (TypeToken, data classes with fields) ──
-keepattributes Signature, *Annotation*, EnclosingMethod
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class com.bushop.sg.data.api.GsonProvider { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Kotlin companion objects with TypeToken ──
-keepclassmembers class **.Companion { *; }

# ── OkHttp / Retrofit internals ──
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Coroutine internals (required for R8 full mode) ──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
