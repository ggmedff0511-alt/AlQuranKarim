
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.bahnabwi.alqurankarim"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bahnabwi.alqurankarim"
        minSdk = 24
        targetSdk = 36
        versionCode = 17
        versionName = "17.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = project.findProperty("RELEASE_STORE_FILE")?.toString()
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.media)
    
    // Room
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)

    // Networking & JSON
    implementation(libs.okhttp)
    implementation(libs.gson)

    // دعم الويب المتقدم للعمل أوفلاين
    implementation("androidx.webkit:webkit:1.12.1")
    
    // مكتبة مواقيت الصلاة للحسابات الأوفلاين
    implementation("com.batoulapps.adhan:adhan:1.2.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.appcompat)
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}

tasks.register("fixRes") {
    doLast {
        val rawDir = project.file("src/main/res/raw")
        if (rawDir.exists()) {
            val swsw = project.file("src/main/res/raw/swsw")
            if (swsw.exists()) swsw.deleteRecursively()
            
            rawDir.listFiles()?.forEach { f ->
                if (f.isFile) {
                    val n = f.name
                    var target: String? = null
                    
                    if (n.contains("\u0646\u063a\u0645\u0629")) target = "salli_ala_mohammad.mp3"
                    else if (n.contains("\u0627\u0644\u0635\u0644\u0627\u0629")) target = "salat_maher.mp3"
                    else if (n.contains("\u0625\u0646_\u0627\u0644\u0644\u0647")) target = "in_allah_yasser.mp3"
                    else if (n.contains("Maher")) target = "salat_maher.mp3"
                    
                    if (target == null && (n.any { it.isUpperCase() || it == ' ' || it.toInt() > 127 })) {
                         val base = n.substringBeforeLast(".")
                         val ext = n.substringAfterLast(".", "")
                         target = base.lowercase().replace(Regex("[^a-z0-9]"), "_") + "." + ext
                    }

                    if (target != null) {
                        val targetFile = project.file("src/main/res/raw/$target")
                        if (targetFile.exists() && f.absolutePath != targetFile.absolutePath) {
                            targetFile.delete()
                        }
                        f.renameTo(targetFile)
                        println("Renamed $n to $target")
                    }
                }
            }
        }
    }
}
