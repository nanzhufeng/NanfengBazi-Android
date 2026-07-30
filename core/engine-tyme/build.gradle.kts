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
    implementation(project(":core:solar-time"))
    implementation("cn.6tail:tyme4j:1.5.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("cn.6tail:lunar:1.7.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

tasks.test {
    useJUnit()
}
