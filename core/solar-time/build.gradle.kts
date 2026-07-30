plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":core:domain"))
    implementation("net.e175.klaus:solarpositioning:2.0.12")

    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
