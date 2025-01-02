# 不记录警告信息
-dontwarn io.citmina.proxychat.**

# 保留主入口点（您的应用主代码）
-keep class io.citmina.proxychat.** { *; }

# 禁用注释移除和字段优化
-keepattributes *Annotation*

# 启用优化选项（可选，根据需要开启）
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# 如果需要，您可以明确指定混淆的排除项
-dontwarn com.squareup.okhttp.**
-dontwarn kotlin.**