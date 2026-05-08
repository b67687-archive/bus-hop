# BusHop-SG ProGuard Rules

# Keep data model classes for Gson serialization
-keep class com.bushop.sg.data.model.** { *; }

# Keep coroutine internals (required for R8 full mode)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Retrofit interfaces
-keep,allowobfuscation interface com.bushop.sg.data.api.ArrivelahApi
