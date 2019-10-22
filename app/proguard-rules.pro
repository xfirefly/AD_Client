# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep all Fragments in this package, which are used by reflection.
-keep class com.android.settings.*Fragment
-keep class com.android.settings.*Picker
-keep class com.android.settings.*Settings
-keep class com.android.settings.wifi.*Settings
-keep class com.android.settings.deviceinfo.*
-keep class com.android.settings.bluetooth.*
-keep class com.android.settings.applications.*
-keep class com.android.settings.inputmethod.*
-keep class com.android.settings.MasterClear
-keep class com.android.settings.MasterClearConfirm
-keep class com.android.settings.accounts.*
-keep class com.android.settings.fuelgauge.*
-keep class cs.comm.** { *;}
-keep class cs.ipc.** { *;}
-keep class org.joda.** { *; }
-keep class com.realtek.** { *; }
-keep class android.serialport.SerialPort { *; }
-keep class android.support.** { *; }

-dontwarn android.support.**
-dontwarn android.support.v4.**
-dontwarn org.jaudiotagger.**
-dontwarn com.realtek.**
-dontwarn org.joda.**

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature


# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }


# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }


##---------------End: proguard configuration for Gson  ----------