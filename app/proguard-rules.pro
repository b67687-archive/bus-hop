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
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keep class com.google.gson.reflect.TypeToken { *; }
# Keep anonymous TypeToken subclasses with their full generic signature
-keep class * extends com.google.gson.reflect.TypeToken { *; }
# Prevent R8 from stripping generic type info from TypeToken anonymous classes
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep the companion object TypeToken instances
-keep class com.bushop.sg.data.local.BusStopStorage { *; }
-keep class com.bushop.sg.data.api.GsonProvider { *; }

# ── OkHttp / Retrofit internals ──
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Retrofit method annotations (dynamic proxy) ──
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ── OkHttp services (ServiceLoader for Conscrypt/BouncyCastle) ──
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontnote okhttp3.internal.platform.**

# ── Coroutine internals (required for R8 full mode) ──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Kotlin stdlib keep (required for some inline functions in R8 full mode) ──
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── DataStore / Preferences (used via reflection in some configurations) ──
-keep class * extends androidx.datastore.preferences.core.Preferences { *; }
