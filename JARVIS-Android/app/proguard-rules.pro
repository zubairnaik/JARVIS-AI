-keepattributes *Annotation*
-dontwarn okio.**
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
