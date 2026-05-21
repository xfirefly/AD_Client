# Add project specific ProGuard rules here.

-keep class cs.comm.** { *;}
-keep class cs.ipc.** { *;}
-keep class org.joda.** { *; }

-dontwarn org.jaudiotagger.**
-dontwarn org.joda.**

##---------------Begin: proguard configuration for Gson  ----------
-keepattributes Signature

-keep class sun.misc.Unsafe { *; }

##---------------End: proguard configuration for Gson  ----------
