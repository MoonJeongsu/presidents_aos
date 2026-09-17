# Keep Cauly SDK classes out of obfuscation.
-keep class com.fsn.cauly.** {
    public *;
    protected *;
}
-keep class com.trid.tridad.** {
    public *;
    protected *;
}
-dontwarn android.webkit.**
