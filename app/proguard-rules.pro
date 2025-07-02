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

# 保持应用程序的主要类不被混淆
-keep class compose.iot.MainActivity { *; }
-keep class compose.iot.ui.theme.** { *; }
-keep class compose.iot.mqtt.** { *; }

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

# 保留日志相关类
-keep class android.util.Log { *; }
-keep class org.slf4j.** { *; }
-keep class ch.qos.logback.** { *; }
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**

# 保留 MQTT 相关类
-keep class org.eclipse.paho.client.mqttv3.** { *; }
-keep class org.eclipse.paho.android.service.** { *; }
-dontwarn org.eclipse.paho.client.mqttv3.**
-dontwarn org.eclipse.paho.android.service.**

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


