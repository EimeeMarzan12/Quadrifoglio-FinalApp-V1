// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.7.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false

}
buildscript {
    dependencies {
        // Other dependencies
        classpath ("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.3") // Use the latest version
    }
}
