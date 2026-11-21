import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    id("jacoco")
}

android {
    namespace = "cl.duoc.syncpass"
    compileSdk = 35

    defaultConfig {
        applicationId = "cl.duoc.syncpass"
        // Caso §5.1: compatibilidad desde Android 8.0 (API 26).
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // URL de la API REST institucional. 10.0.2.2 es el host desde el emulador.
        buildConfigField("String", "API_BASE_URL", "\"https://hluamat8vg.execute-api.us-east-1.amazonaws.com/\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            enableUnitTestCoverage = true
            val localApi = providers.gradleProperty("syncpassApiUrl").orNull
            if (localApi != null) {
                require(localApi.startsWith("http://") && localApi.endsWith("/")) {
                    "syncpassApiUrl debe ser una URL HTTP local terminada en /"
                }
                buildConfigField("String", "API_BASE_URL", "\"$localApi\"")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Silencia el opt-in de UnconfinedTestDispatcher/setMain (kotlinx-coroutines-test),
        // usados solo en app/src/test para los ViewModel.
        freeCompilerArgs += "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose + Material Design 3 (caso §5.1)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Clases de tamano de ventana: telefono compacto, tablet medium / expanded
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Imagenes
    implementation(libs.coil.compose)

    // Red: API REST institucional con Retrofit (caso §5.1)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)

    // Persistencia local embebida Room / SQLite para el modo offline-first (caso §3.4)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Camara y lectura de codigos QR en terreno
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

// Las clases de SyncPassRepository/ViewModel aparecian en 0% pese a tests
// pasando: el agente de JaCoCo (on-the-fly) no logra instrumentar las clases
// de continuacion de corrutinas (`$metodo$1`) una vez que el classloader en
// sandbox de Robolectric las recarga. `includeNoLocationClasses` + excluir
// `jdk.internal.*` es el workaround documentado de JaCoCo/Robolectric para
// que esas clases sin "SourceLocation" propio en el sandbox se cuenten igual.
tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

// Caso rubrica: cobertura de linea sobre logica de negocio real. El reporte de
// AGP (enableUnitTestCoverage) cuenta por defecto codigo generado (Compose
// ComposableSingletons, Room *_Impl, BuildConfig/R) y las pantallas @Composable,
// que no son unit-testeables de forma razonable. Se excluyen del denominador
// para que el porcentaje refleje la logica que si tiene tests (ViewModels,
// Repository, modelos), sin tocar ningun comportamiento de la app.
val jacocoCoverageExclusions = listOf(
    "**/*ComposableSingletons*",
    "**/*_Impl*",
    "**/*_Impl$*",
    "**/BuildConfig.*",
    "**/R.class",
    "**/R$*.class",
    "**/MainActivity*",
    "**/ui/theme/**",
    "**/ui/navigation/**",
    "**/ui/layout/**",
    // Los @Composable *Screen viven en el mismo archivo *Screen.kt; el
    // compilador los emite como "<Archivo>ScreenKt*.class" (facade + lambdas
    // anonimas), nunca como "<Archivo>ViewModel*.class", asi que este patron
    // excluye solo la UI y deja intactos los ViewModel del mismo paquete.
    "**/ui/screens/*ScreenKt*.class",
    "**/ui/SyncPassViewModel*", // fabrica de ViewModels basada en @Composable
)

// AGP finaliza `classFileCollection` de su propia tarea
// (createDebugUnitTestCoverageReport) apenas la registra, asi que no se puede
// filtrar desde fuera (Gradle 9 lanza "property ... cannot be changed any
// further" tanto en configuracion como en doFirst). En su lugar se declara
// una tarea JacocoReport (plugin "jacoco" nativo de Gradle) que lee el mismo
// .exec de los unit tests y las mismas carpetas de clases compiladas, pero
// aplicando las exclusiones. Mismos tests, mismo bytecode: solo cambia el
// denominador del reporte.
tasks.register<JacocoReport>("jacocoFilteredTestReport") {
    group = "verification"
    description = "Cobertura de unit tests (debug) excluyendo codigo generado y UI Compose no testeable."
    dependsOn("testDebugUnitTest")

    val kotlinClassesDir = layout.buildDirectory.dir("tmp/kotlin-classes/debug")
    val javacClassesDir = layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
    val execDataDir = layout.buildDirectory.dir("outputs/unit_test_code_coverage/debugUnitTest")

    executionData.setFrom(fileTree(execDataDir) { include("*.exec") })
    classDirectories.setFrom(
        fileTree(kotlinClassesDir) { exclude(jacocoCoverageExclusions) },
        fileTree(javacClassesDir) { exclude(jacocoCoverageExclusions) }
    )
    sourceDirectories.setFrom(files("src/main/java"))

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
