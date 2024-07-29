// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  id("com.android.application") version "8.0.0" apply false
  id("com.android.library") version "8.0.0" apply false
  id("org.jetbrains.kotlin.android") version "1.8.21" apply false
}

buildscript {
  dependencies {
    classpath("com.google.android.gms:oss-licenses-plugin:0.10.6")
  }
}

tasks.register<Delete>("clean") { delete(rootProject.buildDir) }
