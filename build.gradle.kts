// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false

    // For ksp in the build.gradle file
    alias(libs.plugins.ksp) apply false
}