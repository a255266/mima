// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false  // kapt 插件
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.plugin) apply false  // Hilt 插件
}