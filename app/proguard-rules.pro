# Cauly
-keep class com.fsn.cauly.** {
    public *;
    protected *;
}
-keep class com.trid.tridad.** {
    public *;
    protected *;
}
-dontwarn android.webkit.**

# Pangle
-keep class com.bytedance.sdk.** { *; }
-keep class com.pangle.global.** { *; }
-dontwarn com.bytedance.sdk.**
-dontwarn com.pangle.global.**

# Unity Ads
-keep class com.unity3d.ads.** { *; }
-dontwarn com.unity3d.ads.**
