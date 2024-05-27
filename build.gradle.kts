import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("java")
}

buildscript {
    val kotlinVersion by extra("1.9.22") // Definiowanie wersji Kotlin jako właściwości extra
    val agp_version by extra("8.4.0")
    repositories {
        google() // Dodaj repozytorium Google
        mavenCentral()
        maven("https://kotlin.bintray.com/kotlinx") // Dodaj repozytorium JetBrains
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$agp_version") // Użyj najnowszej dostępnej wersji
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

allprojects {
    repositories {
        google() // Dodaj repozytorium Google
        mavenCentral()
    }
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}




