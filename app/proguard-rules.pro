# ─────────────────────────────────────────────────────────────────────────────
# Monityx ProGuard / R8 rules
# ─────────────────────────────────────────────────────────────────────────────

# ---------- Kotlin ----------
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- Hilt / Dagger ----------
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
# AssistedInject (used by HiltWorker) — keep generated factories
-keep @dagger.assisted.AssistedFactory interface *
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# ---------- Room ----------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ---------- Retrofit + OkHttp ----------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
# Keep the Retrofit service interface
-keep interface com.bansalcoders.monityx.data.remote.** { *; }

# ---------- Gson / JSON DTOs ----------
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Keep all remote DTOs used for JSON deserialization
-keep class com.bansalcoders.monityx.data.remote.dto.** { *; }
# Prevent stripping generic type info (needed by Gson's TypeToken)
-keepattributes Signature

# ---------- Coroutines ----------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---------- WorkManager + HiltWorker ----------
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
# Keep Hilt worker factory bridge
-keep class androidx.hilt.work.HiltWorkerFactory { *; }
-keep @androidx.hilt.work.HiltWorker class * { *; }

# ---------- DataStore ----------
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ---------- Compose ----------
# R8 handles Compose correctly; only suppress known false-positive warnings.
-dontwarn androidx.compose.**

# ---------- Security Crypto ----------
-keep class androidx.security.crypto.** { *; }
