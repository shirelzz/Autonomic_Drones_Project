-keepattributes Exceptions,InnerClasses,*Annotation*,Signature,EnclosingMethod

-dontoptimize
-dontpreverify
-dontwarn okio.**
-dontwarn org.bouncycastle.**
-dontwarn dji.**
-dontwarn com.dji.**
-dontwarn sun.**
-dontwarn java.**
-dontwarn com.amap.api.**
-dontwarn com.here.**
-dontwarn com.mapbox.**
-dontwarn okhttp3.**
-dontwarn retrofit2.**

-keepclassmembers enum * {
    public static <methods>;
}

-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep class * extends android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keep,allowshrinking class * extends dji.publics.DJIUI.** {
    public <methods>;
}

# Preserve all annotations
-keepattributes *Annotation*

# Preserve the special class members needed in all classes
-keep class * {
    <init>();
}

# Preserve Chaquopy Python-related classes
-keep class **$py { *; }

# Add specific rules for the classes reported in the errors
-keep class csv$py { *; }
-keep class email.mime.audio$py { *; }
-keep class email.charset$py { *; }
-keep class Lib.csv$py { *; }
-keep class Lib.email.charset$py { *; }
-keep class unittest.__main__$py { *; }

#/* Add similar rules for any other classes reported in the errors */
-keep class Lib.modjy.modjy_response$py { *; }
-keep class encodings.utf_16_le$py { *; }
-keep class distutils.tests.test_versionpredicate$py { *; }
-keep class json.tests.test_float$py { *; }
-keep class distutils.tests.test_install_lib$py { *; }
-keep class Lib.email.utils$py { *; }
-keep class Lib._google_ipaddr_r234$py { *; }
-keep class Lib.distutils.tests.test_install_lib$py { *; }
-keep class Lib.distutils.spawn$py { *; }
-keep class Lib.distutils.tests.test_config$py { *; }
-keep class Lib.lib2to3.fixes.fix_numliterals$py { *; }
-keep class Lib.modjy.modjy_params$py { *; }
-keep class Lib.inspect$py { *; }
-keep class Lib.json.tests.test_default$py { *; }
-keep class Lib.tempfile$py { *; }
-keep class Lib.encodings.shift_jis$py { *; }
-keep class Lib.lib2to3.fixes.fix_long$py { *; }
#### end of Chaquopy

-keep class net.sqlcipher.database.* { *; }

-keep class dji.** { *; }

-keep class com.dji.** { *; }

-keep class com.google.** { *; }

-keep class org.bouncycastle.** { *; }

-keep,allowshrinking class org.** { *; }

-keep class com.squareup.wire.** { *; }

-keep class sun.misc.Unsafe { *; }

-keep class com.secneo.** { *; }

-keep class org.greenrobot.eventbus.**{*;}

-keepclasseswithmembers,allowshrinking class * {
    native <methods>;
}

-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keep class androidx.appcompat.widget.SearchView { *; }

-keepclassmembers class * extends android.app.Service
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
-keep class androidx.** { *; }
-keep class android.media.** { *; }
-keep class okio.** { *; }
-keep class com.lmax.disruptor.** {
    *;
}

-dontwarn com.mapbox.services.android.location.LostLocationEngine
-dontwarn com.mapbox.services.android.location.MockLocationEngine
-keepclassmembers class * implements android.arch.lifecycle.LifecycleObserver {
    <init>(...);
}
# ViewModel's empty constructor is considered to be unused by proguard
-keepclassmembers class * extends android.arch.lifecycle.ViewModel {
    <init>(...);
}
# keep Lifecycle State and Event enums values
-keepclassmembers class android.arch.lifecycle.Lifecycle$State { *; }
-keepclassmembers class android.arch.lifecycle.Lifecycle$Event { *; }
# keep methods annotated with @OnLifecycleEvent even if they seem to be unused
# (Mostly for LiveData.LifecycleBoundObserver.onStateChange(), but who knows)
-keepclassmembers class * {
    @android.arch.lifecycle.OnLifecycleEvent *;
}

-keepclassmembers class * implements android.arch.lifecycle.LifecycleObserver {
    <init>(...);
}

-keep class * implements android.arch.lifecycle.LifecycleObserver {
    <init>(...);
}
-keepclassmembers class android.arch.** { *; }
-keep class android.arch.** { *; }
-dontwarn android.arch.**
