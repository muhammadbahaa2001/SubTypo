import com.android.build.gradle.BaseExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin) apply false
}

fun Project.configureBaseExtension() {
  extensions.findByType(BaseExtension::class)?.run {
    compileSdkVersion(34)
    buildToolsVersion = "34.0.0"

    defaultConfig {
      minSdk = 24
      targetSdk = 34
      versionCode = 2
      versionName = "1.1.1"
    }

    compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
    }
  }
}

subprojects {
  plugins.withId("com.android.application") { configureBaseExtension() }
  plugins.withId("com.android.library") { configureBaseExtension() }
}

tasks.register<Delete>("clean") { delete(rootProject.buildDir) }
