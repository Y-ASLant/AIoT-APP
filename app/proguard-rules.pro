# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile



# 保持 native 方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保持枚举类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保持 Serializable 类和成员不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ── Timber：Release 构建中通过 R8 彻底移除日志调用 ──
# 删除所有 Timber 日志方法调用（Debug 日志不会出现在 Release APK 中）
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
    public static void w(...);
    public static void e(...);
    public static void wtf(...);
}

# 同时也删除 android.util.Log 的残留调用（以防万一）
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# 保留 HiveMQ 和被引用的可选库
-dontwarn com.hivemq.client.**
-dontwarn reactor.**
-dontwarn org.jctools.**
-dontwarn io.netty.**
-dontwarn org.slf4j.**

# 保留 META-INF 服务文件
-keep,allowobfuscation class * implements com.android.tools.lint.client.api.IssueRegistry
-keep class com.android.tools.lint.client.api.IssueRegistry
-keepattributes *Annotation*
-keepattributes Signature
-dontwarn com.android.tools.lint.**
-keep class META-INF.services.** { *; }
-keepclassmembers class * {
    @javax.annotation.* *;
}


