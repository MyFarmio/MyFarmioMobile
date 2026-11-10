import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.myfarmio.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.myfarmio.app"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Configure in local.properties (ignored by Git); never commit project keys.
        val mobileLocalProperties = Properties().apply {
            val localFile = rootProject.file("local.properties")
            if (localFile.isFile) localFile.inputStream().use { load(it) }
        }
        val supabaseUrl = providers.gradleProperty("MYFARMIO_SUPABASE_URL")
            .orElse(providers.environmentVariable("MYFARMIO_SUPABASE_URL"))
            .orElse(mobileLocalProperties.getProperty("MYFARMIO_SUPABASE_URL", "")).get()
        val supabaseAnonKey = providers.gradleProperty("MYFARMIO_SUPABASE_ANON_KEY")
            .orElse(providers.environmentVariable("MYFARMIO_SUPABASE_ANON_KEY"))
            .orElse(mobileLocalProperties.getProperty("MYFARMIO_SUPABASE_ANON_KEY", "")).get()
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.airbnb.android:lottie:6.7.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Navegación
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // HTTP / Supabase REST
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // SharedPreferences encriptado
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Imágenes
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Fechas
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.6")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.3")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime:2.8.3")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.work:work-runtime:2.9.1")
}
