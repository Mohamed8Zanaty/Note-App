// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Use compatible versions
        classpath("com.android.tools.build:gradle:8.13.0") // Stable version
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0") // Match Kotlin version
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.7") // Updated to match nav version
    }
}



tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}