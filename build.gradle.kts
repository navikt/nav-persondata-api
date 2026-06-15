import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.graphql.kotlin)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.spotless)
    alias(libs.plugins.sonarqube)
    jacoco
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
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
            "library" to "spring-boot", // tryggere enn spring-cloud for å unngå swagger
            "useSwaggerAnnotations" to "false",
            "useSpringBoot3" to "true",
        ),
    )
}

val graphqlGeneratePdlClient by tasks.registering(GraphQLGenerateClientTask::class) {
    packageName.set("no.nav.persondataapi.generated.pdl")
    schemaFile.set(file("src/main/resources/graphql/schema/pdl/pdl-api-sdl.graphqls"))
    queryFileDirectory.set(file("src/main/resources/graphql/queries/pdl"))
    allowDeprecatedFields.set(false)
}

val graphqlGenerateNomClient by tasks.registering(GraphQLGenerateClientTask::class) {
    packageName.set("no.nav.persondataapi.generated.nom")
    schemaFile.set(file("src/main/resources/graphql/schema/nom/nom-api.graphqls"))
    queryFileDirectory.set(file("src/main/resources/graphql/queries/nom"))
    allowDeprecatedFields.set(false)
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
    "build/generate-resources/main/src/main/kotlin",
)

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

tasks.named<Jar>("bootJar") {
    archiveFileName.set("app.jar")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    doFirst {
        val agentJar = configurations.testRuntimeClasspath.get().find { it.name.contains("byte-buddy-agent") }
        jvmArgs("-javaagent:$agentJar")
    }
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.withType<Test>())
    reports {
        xml.required.set(true)
    }
}

sonar {
    properties {
        property("sonar.projectKey", "navikt_nav-persondata-api")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.sources", "src/main/kotlin")
        property("sonar.tests", "src/test/kotlin")
        property("sonar.exclusions", "build/generated/**")
    }
}

tasks.named("compileKotlin") {
    dependsOn(graphqlGeneratePdlClient, graphqlGenerateNomClient, "openApiGenerate")
}

// Generer en markdown-fil med versjoner av viktige avhengigheter
// Før kjøring/regenerering, slett docs/versions.md hvis den finnes
// kjør ./gradlew generateVersionInfo
tasks.register("generateVersionInfo") {
    doLast {
        val catalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

        fun ver(alias: String) = catalog.findVersion(alias).get().requiredVersion

        val versions =
            mapOf(
                "Java" to
                    java.toolchain.languageVersion
                        .get()
                        .asInt()
                        .toString(),
                "Gradle" to gradle.gradleVersion,
                "Spring Boot" to ver("spring-boot"),
                "Kotlin" to ver("kotlin"),
                "Coroutines" to ver("coroutines"),
                "Token Support" to ver("token-support"),
                "GraphQL Kotlin" to ver("graphql-kotlin"),
                "WireMock" to ver("wiremock"),
                "Springdoc" to ver("springdoc"),
            )

        val content = versions.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }

        file("docs/versions.md").writeText(content)
    }
}

dependencies {
    // BOM styrer transitive versjoner for Spring Boot-avhengigheter
    implementation(platform(libs.spring.boot.bom))

    implementation(libs.commons.lang3) {
        version { strictly("3.18.0") }
        because("Fixes CVE-2025-48924")
    }

    implementation(libs.jackson.module.kotlin)

    // Tracing (Micrometer → OpenTelemetry)
    implementation(libs.micrometer.tracing.otel)
    runtimeOnly(libs.opentelemetry.otlp)

    // Strukturerte JSON-logger (til stdout -> NAIS logger)
    implementation(libs.logstash.encoder)

    implementation(libs.reactor.netty)
    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.cache)
    implementation(libs.spring.boot.data.redis)
    implementation(libs.caffeine)

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.coroutines.slf4j)
    implementation(libs.micrometer.prometheus)

    testImplementation(libs.spring.boot.test) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.platform.launcher)
    // WireMock for HTTP boundary stubbing in tests
    testImplementation(libs.wiremock)

  /*
  WebFlux
  Dette gir deg WebClient, men ikke hele WebFlux runtime.
🔸  Ingen Netty, ingen Reactor context, og ingen konflikt med Spring MVC
   */
    implementation(libs.spring.webflux)
  /*
   * Sikkerhet
   * */
    implementation(libs.spring.boot.validation)
    implementation(libs.token.validation.spring)
    implementation(libs.token.client.spring)
    implementation(libs.kotlin.stdlib)
    implementation(libs.graphql.client.jackson)
    implementation(libs.graphql.client)
    implementation(libs.graphql.spring.client)

    // Swagger UI og OpenAPI-dokumentasjon
    implementation(libs.springdoc.openapi)
    implementation(libs.swagger.annotations)
    implementation(libs.swagger.models)
}
