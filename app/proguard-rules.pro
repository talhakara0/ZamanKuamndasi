# ========================================
# ZAMAN KUMANDASI PROGUARD RULES
# ========================================

# ========================================
# GENEL AYARLAR
# ========================================

# Hata ayıklama için satır numaralarını koru
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Genel optimizasyon ayarları
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# ========================================
# ANDROID FRAMEWORK
# ========================================

# Android framework sınıflarını koru
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View

# Android manifest sınıflarını koru
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ========================================
# UYGULAMA MODEL SINIFLARI
# ========================================

# Tüm model sınıflarını koru (Firebase, Room, vb.)
-keep class com.talhadev.zamankumandasi.data.model.** { *; }
-keep class com.talhadev.zamankumandasi.data.entity.** { *; }

# ========================================
# FIREBASE & GOOGLE SERVICES
# ========================================

# Firebase tüm sınıfları
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.android.gms.measurement.** { *; }

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.android.gms.auth.** { *; }

# Firebase Database
-keep class com.google.firebase.database.** { *; }

# ========================================
# ADMOB & REKLAM
# ========================================

# AdMob sınıfları
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# AdMob Mediation
-keep class com.google.android.gms.ads.mediation.** { *; }

# ========================================
# ROOM DATABASE
# ========================================

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *

# Room annotations
-keep @interface androidx.room.*
-keep class androidx.room.** { *; }

# ========================================
# HILT/DAGGER DEPENDENCY INJECTION
# ========================================

# Hilt/Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.** { *; }

# Hilt annotations
-keep @interface dagger.hilt.** { *; }
-keep @interface javax.inject.** { *; }

# ========================================
# WORKMANAGER
# ========================================

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# ========================================
# BILLING & IN-APP PURCHASE
# ========================================

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# ========================================
# NAVIGATION COMPONENT
# ========================================

# Navigation Component
-keep class androidx.navigation.** { *; }

# ========================================
# LIFECYCLE & VIEWMODEL
# ========================================

# Lifecycle
-keep class androidx.lifecycle.** { *; }

# ========================================
# COROUTINES
# ========================================

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }

# ========================================
# PARCELABLE & SERIALIZABLE
# ========================================

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========================================
# NATIVE METHODS
# ========================================

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ========================================
# ENUMS
# ========================================

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========================================
# EXCEPTIONS
# ========================================

# Custom exceptions
-keep public class * extends java.lang.Exception

# ========================================
# REFLECTION
# ========================================

# Reflection kullanımı için gerekli sınıflar
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ========================================
# ACCESSIBILITY SERVICE
# ========================================

# Accessibility Service
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# ========================================
# LOGGING REMOVAL
# ========================================

# Release build'de log mesajlarını kaldır
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# ========================================
# PERFORMANCE OPTIMIZATIONS
# ========================================

# Gereksiz kodları kaldır
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**
-dontwarn androidx.room.**

# ========================================
# FINAL OPTIMIZATIONS
# ========================================

# Son optimizasyonlar
-printmapping mapping.txt
-printseeds seeds.txt
-printusage unused.txt