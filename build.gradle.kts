import com.expediagroup.graphql.plugin.gradle.graphql
val tokenSupportVersion = "5.0.30"
val latestGraphQLKotlinVersion = "9.0.0-alpha.8"


plugins {
    kotlin("jvm") version "1.9.24"
    id("org.springframework.boot") version "3.2.7"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.spring") version "1.9.24"
    id("com.expediagroup.graphql") version "9.0.0-alpha.8"
    id("org.openapi.generator") version "7.0.1"

}

openApiGenerate {
    inputSpec.set("src/main/resources/openapi/ikomp-inntektshistorikk-api-2.1.2-swagger.json")
    generatorName.set("kotlin-spring")
    apiPackage.set("no.nav.inntekt.generated.api")
    modelPackage.set("no.nav.inntekt.generated.model")
    configOptions.set(
        mapOf(
            "useSpringWebClient" to "true",
            "interfaceOnly" to "true",
            "library" to "spring-boot",  // tryggere enn spring-cloud for å unngå swagger
            "useSwaggerAnnotations" to "false",
            "useSpringBoot3" to "true"
        )
    )

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
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

sourceSets["main"].kotlin.srcDirs(
    "build/generated/source/graphql/main",
    "build/generate-resources/main/src/main/kotlin")

// Sørg for at generateJava kjører før build
tasks.named("build") {

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
    dependsOn("graphqlGenerateClient","openApiGenerate")
}
dependencies {
    //jacson
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.7"))
    implementation(enforcedPlatform("com.fasterxml.jackson:jackson-bom:2.17.1"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Tracing (Micrometer → OpenTelemetry)
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")

    // Strukturerte JSON-logger (til stdout -> NAIS logger)
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    implementation("io.projectreactor.netty:reactor-netty-http")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.8.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.8")
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
    implementation("com.expediagroup:graphql-kotlin-client-jackson:9.0.0-alpha.8")
    implementation("com.expediagroup:graphql-kotlin-client:9.0.0-alpha.8")
    implementation("com.expediagroup:graphql-kotlin-client-generator:${latestGraphQLKotlinVersion}")
    implementation("com.expediagroup:graphql-kotlin-client:${latestGraphQLKotlinVersion}")
    implementation("com.expediagroup:graphql-kotlin-spring-client:${latestGraphQLKotlinVersion}")

    implementation("io.swagger.core.v3:swagger-annotations:2.2.39")
    implementation("io.swagger.core.v3:swagger-models:2.2.39")

}
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.fasterxml.jackson.core" ||
            requested.group == "com.fasterxml.jackson.module" ||
            requested.group == "com.fasterxml.jackson.datatype" ||
            requested.group == "com.fasterxml.jackson") {

            useVersion("2.17.1")
            because("Kotlin 2.0 krever jackson-module-kotlin >= 2.17.0")
        }
    }
}
