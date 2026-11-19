# 保留 JSON 序列化模型字段名（防止 release 混淆破坏 LLM 协议）
-keepclassmembers class com.ziweistar.app.engine.model.** { *; }
-keep class com.nlf.calendar.** { *; }
-dontwarn com.nlf.calendar.**
-keep class net.sqlcipher.** { *; }
