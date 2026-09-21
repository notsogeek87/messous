plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.budgetflow.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.budgetflow.app"
        minSdk = 26
        targetSdk = 35
        // Surchargeable en CI via -PbudgetflowVersionCode=N -PbudgetflowVersionName=X.Y.N
        // pour que chaque release GitHub porte une version unique et croissante.
        versionCode = if (project.hasProperty("budgetflowVersionCode")) {
            (project.property("budgetflowVersionCode") as String).toInt()
        } else {
            1
        }
        versionName = if (project.hasProperty("budgetflowVersionName")) {
            project.property("budgetflowVersionName") as String
        } else {
            "1.0.0"
        }

        vectorDrawables.useSupportLibrary = true
    }

    // Clé debug FIXE, committée dans le repo (app/debug.keystore).
    // Sans ça, chaque runner CI neuf régénère sa propre clé debug aléatoire
    // (~/.android/debug.keystore n'existe pas encore) : deux builds
    // successifs sont alors signés différemment, et Android refuse
    // d'installer la mise à jour par-dessus l'ancienne ("app non installée")
    // tant qu'on n'a pas désinstallé à la main. Une clé debug n'a rien de
    // secret (mot de passe "android" documenté par Google) — la committer
    // est la pratique standard pour des builds CI reproductibles.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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

    sourceSets {
        // Lets local unit tests load assets/services.json as a plain classpath resource
        // (Robolectric-free), so the service catalog is validated on every `test` run.
        getByName("test") {
            resources.srcDirs("src/main/assets")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core-engine"))

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Fournit les styles XML Theme.Material3.* (thème du manifeste et du
    // splash screen), distincts du Material3 Compose importé au-dessus.
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.biometric:biometric:1.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
