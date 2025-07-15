import com.expediagroup.graphql.plugin.gradle.graphql
val tokenSupportVersion = "5.0.30"
val latestGraphQLKotlinVersion = "9.0.0-alpha.8"


plugins {
    kotlin("jvm") version "1.9.22"
    id("org.springframework.boot") version "3.2.7"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("plugin.spring") version "1.9.22"
    id("com.expediagroup.graphql") version "9.0.0-alpha.8"


}
graphql {
    client {

        packageName = "no.nav.persondataapi.generated"
        // For statiske spørringer
        queryFileDirectory = "src/main/resources/graphql/queries"
        schemaFile = file("src/main/resources/graphql/schema/pdl-api-sdl.graphqls")

    }
}

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

sourceSets["main"].kotlin.srcDir("build/generated/source/graphql/main")

// Sørg for at generateJava kjører før build
tasks.named("build") {
    //dependsOn("generateJava")
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "21"
}
tasks.named<Jar>("bootJar") {
    archiveFileName.set("app.jar")
}
tasks.withType<Test> {
    useJUnitPlatform()
}





tasks.named("compileKotlin") {
    dependsOn("graphqlGenerateClient")
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
        implementation(kotlin("stdlib"))
    implementation("com.expediagroup:graphql-kotlin-client-generator:${latestGraphQLKotlinVersion}")
    implementation("com.expediagroup:graphql-kotlin-client:${latestGraphQLKotlinVersion}")
        implementation("com.expediagroup:graphql-kotlin-spring-client:${latestGraphQLKotlinVersion}")

}