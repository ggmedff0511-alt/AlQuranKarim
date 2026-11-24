# ProGuard rules for AlQuranKarim

# Keep the JavaScript Interface for WebView
-keepclassmembers class com.bahnabwi.alqurankarim.MainActivity$WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep the @JavascriptInterface annotation
-keepattributes JavascriptInterface

# Optional: Keep common Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Support for Kotlin (if used in libraries)
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Prevent shrinking of resources used in strings or via reflection if necessary
-keepclassmembers class **.R$* {
    public static <fields>;
}