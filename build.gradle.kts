import io.gitlab.arturbosch.detekt.Detekt
import kotlinx.kover.api.CounterType
import kotlinx.kover.api.IntellijEngine
import kotlinx.kover.api.KoverMergedFilters
import kotlinx.kover.api.KoverProjectConfig
import kotlinx.kover.api.KoverTaskExtension
import kotlinx.kover.api.KoverVerifyConfig
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestReport
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.sonarqube.gradle.SonarExtension
import org.sonarqube.gradle.SonarProperties

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.8.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.55")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.1.10")
        classpath("com.google.gms:google-services:4.4.2")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.3")
        classpath("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.5.0.2730")
    }
}

val liba = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("org.sonarqube") version "6.0.1.5171"
}

val Test.isDev get() = name.contains("debug", ignoreCase = true)

fun Project.catalogVersion(alias: String) = liba.findVersion(alias).get().toString()
fun Project.catalogLib(alias: String) = liba.findLibrary(alias).get()

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
    "*.theme.*",
)

val codeExclusions = listOf(
    "**/R.*",
    "**/R$*.*",
    "**/BuildConfig.*",
)

allprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.sonarqube")

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
    }

    dependencies {
        detektPlugins(catalogLib("detekt-compose"))
        detektPlugins(catalogLib("detekt-compose2"))
        detektPlugins(catalogLib("detekt-formatting"))
    }

    extensions.configure<SonarExtension> {
        properties {
            addFilesIfExist("sonar.android.lint.report", "$buildDir/reports/lint-results-debug.xml")
            addFilesIfExist(
                "sonar.coverage.jacoco.xmlReportPaths",
                "$buildDir/reports/kover/xml/report.xml",
            )
            addFilesIfExist("sonar.java.binaries", "$buildDir/tmp/kotlin-classes/debug")
            addFilesIfExist("sonar.junit.reportPaths", "$buildDir/test-results/testDebugUnitTest")
            addFilesIfExist("sonar.sources", "$projectDir/src/main/kotlin")
            addFilesIfExist("sonar.tests", "$projectDir/src/test/kotlin")
            addFilesIfExist(
                "sonar.kotlin.detekt.reportPaths",
                "$buildDir/reports/detekt/detekt.xml",
            )

            property("sonar.host.url", "https://sonarcloud.io")
            property("sonar.organization", "victorhvs")
            property("sonar.projectKey", "VictorHVS_TradingFight.club")
            property("sonar.projectName", "TradingFight.club")
            property("sonar.coverage.exclusions", koverExcludes.joinToString(separator = ","))
            property("sonar.exclusions", codeExclusions.joinToString(separator = ","))
            property("sonar.pullrequest.provider", "GitHub")
            property("sonar.pullrequest.github.repository", "VictorHVS/trading-fight-club-mobile")
            property("sonar.java.coveragePlugin", "jacoco")
            property("sonar.language", "kotlin")
            property("sonar.log.level", "TRACE")
            property("sonar.qualitygate.wait", true)
            property("sonar.sourceEncoding", "UTF-8")
            property("sonar.tags", "android")
            property("sonar.verbose", true)
        }
    }
}

fun SonarProperties.addFilesIfExist(property: String, vararg paths: String) {
    paths.filter { file ->
        File(file).exists()
    }.takeUnless { files ->
        files.isEmpty()
    }?.joinToString(separator = ",")?.let { files ->
        property(property, files)
    }
}

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
            minValue = 0
        }
    }
    rule {
        name = "Minimal line coverage rate in percent"
        bound {
            counter = CounterType.LINE
            minValue = 0
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

@Suppress("UnstableApiUsage")
tasks.register("unitTests", TestReport::class) {
    val testTasks = subprojects.map { p ->
        p.tasks.withType<Test>().matching { t -> t.isDev }
    }

    mustRunAfter(testTasks)
    destinationDirectory.set(file("$buildDir/reports/allTests"))
    testResults.setFrom(testTasks)
}

tasks.register("detektAll", Detekt::class) {
    description = "Run detekt in all modules"

    parallel = true
    ignoreFailures = true
    autoCorrect = true
    buildUponDefaultConfig = true
    jvmTarget = JavaVersion.VERSION_1_8.toString()
    setSource(files(projectDir))
    config.setFrom(files("$rootDir/config/detekt.yml"))
    include("**/*.kt", "**/*.kts")
    exclude("**/resources/**", "**/build/**")

    reports {
        html.required.set(true)
        sarif.required.set(true)
        txt.required.set(false)
        xml.required.set(true)
    }
}
