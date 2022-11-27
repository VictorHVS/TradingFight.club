import kotlinx.kover.api.CounterType
import kotlinx.kover.api.IntellijEngine
import kotlinx.kover.api.KoverMergedConfig
import kotlinx.kover.api.KoverMergedFilters
import kotlinx.kover.api.KoverProjectConfig
import kotlinx.kover.api.KoverTaskExtension
import kotlinx.kover.api.KoverVerifyConfig
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.44")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.7.10")
        classpath("com.google.gms:google-services:4.3.14")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.2")
//        classpath("org.jacoco:org.jacoco.core:0.8.8")
    }
}

plugins {
    id("org.jetbrains.kotlinx.kover") version "0.6.1"
}

val liba = extensions.getByType<VersionCatalogsExtension>().named("libs")

val Test.isDev get() = name.contains("debug", ignoreCase = true)

fun Project.catalogVersion(alias: String) = liba.findVersion(alias).get().toString()

val koverIncludes = listOf("com.victorhvs.tfc.*")
val koverExcludes = listOf(
    // App
    "*.TFCApp",
    "*.initializers.*",

    // Apollo
    "*.remote.*.adapter.*",
    "*.remote.*.fragment.*",
    "*.remote.*.selections.*",
    "*.remote.*.type.*",
    "*.remote.*.*Mutation*",
    "*.remote.*.*Query*",

    // Common
    "*.common.*",

    // Common Android
    "*.BuildConfig",
    "*.*Activity",
    "*.*Fragment",
    "*.base.*",
    "*.navigation.*",

    // Compose
    "*.*ComposableSingletons*",

    // Hilt
    "*.di.*",
    "*.*Hilt_*",
    "*.*HiltModules*",
    "*.*_Factory",

    // Serializers
    "*$\$serializer",

    // Ui
    "*.ui.*.components.*",
    "*.ui.*.view.*",
)

allprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")

    group = "com.victorvs.tfc"
    version = "0.1.0"

    val engineVersion = catalogVersion("coverage-engine")

    extensions.configure<KoverProjectConfig> {
        engine.set(IntellijEngine(engineVersion))
        filters {
            classes {
                excludes.addAll(koverExcludes)
                includes.addAll(koverIncludes)
            }
        }
    }

    tasks.withType<Test> {
        extensions.configure<KoverTaskExtension> {
            isDisabled.set(!isDev)
        }
    }

    koverMerged {
        enable()
        filters { commonFilters() }
        verify { rules() }

//        xmlReport {
//            onCheck.set(false)
//            reportFile.set(layout.buildDirectory.file("$buildDir/reports/kover/result.xml"))
//        }
//        htmlReport {
//            onCheck.set(false)
//            reportDir.set(layout.buildDirectory.dir("$buildDir/reports/kover/html-result"))
//        }
    }
}

val MIN_COVERED_PERCENTAGE = 80

fun KoverMergedFilters.commonFilters() {
    classes {
        excludes.addAll(koverExcludes)
        includes.addAll(koverIncludes)
    }
}

fun KoverVerifyConfig.rules() {
    rule {
        name = "Minimal instruction coverage rate in percent"
        bound {
            counter = CounterType.INSTRUCTION
            minValue = MIN_COVERED_PERCENTAGE
        }
    }
    rule {
        name = "Minimal line coverage rate in percent"
        bound {
            counter = CounterType.LINE
            minValue = MIN_COVERED_PERCENTAGE
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}