val tokenSupportVersion = "5.0.30"
plugins {
    id("org.springframework.boot") version "3.2.7"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
}

group = "no.nav.persondata-api"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "21"
}
tasks.named<Jar>("bootJar") {
    archiveFileName.set("app.jar")
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("io.micrometer:micrometer-registry-prometheus")

    /*
    WebFlux
    Dette gir deg WebClient, men ikke hele WebFlux runtime.
🔸  Ingen Netty, ingen Reactor context, og ingen konflikt med Spring MVC
     */
    implementation("org.springframework:spring-webflux")
    /*
    * Sikkerhet
    * */
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("no.nav.security:token-validation-spring:${tokenSupportVersion}")
    implementation("no.nav.security:token-client-spring:${tokenSupportVersion}")

}

tasks.withType<Test> {
    useJUnitPlatform()
}