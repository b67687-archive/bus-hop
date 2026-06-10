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
# Keep ALL methods and their generic signatures (critical for Kotlin suspend functions:
# Retrofit's HttpServiceMethod does an unchecked (ParameterizedType) cast on
# getGenericParameterTypes().last() — if R8 strips the Signature attribute,
# getGenericParameterTypes() returns raw Class instead of ParameterizedType.)
-keep interface com.bushop.sg.data.api.ArrivelahApi { *; }

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

# ── Gson (complete) ──
# Preserve all generic type signatures (critical for Gson's TypeToken reflection)
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
# Keep ALL Gson classes — R8 must not strip, obfuscate, or optimize any of them
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }
-keep enum com.google.gson.** { *; }
# Specifically TypeToken and all subclasses (anonymous or otherwise)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
# Keep @SerializedName on all fields (used by Gson reflection)
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep our Gson provider and storage classes
-keep class com.bushop.sg.data.local.BusStopStorage { *; }
-keep class com.bushop.sg.data.api.GsonProvider { *; }
# Prevent R8 from stripping generic type info from any method that uses Gson types
-keepclassmembers class * {
    @com.google.gson.annotations.Expose <fields>;
}

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

# ── Kotlin coroutines Continuation (preserve generic type param for Retrofit reflection) ──
-keepattributes Signature
-keep class kotlin.coroutines.Continuation { *; }

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
