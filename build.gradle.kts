import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val tokenSupportVersion = "5.0.30"
val graphQLKotlinVersion = "9.0.0-alpha.8"
val springBootVersion = "4.0.3"
val jacksonVersion = "2.17.1"
val kotlinVersion = "2.2.20"
val coroutinesVersion = "1.10.2"


plugins {
  kotlin("jvm") version "2.3.0"
  id("org.springframework.boot") version "4.0.2"
  id("io.spring.dependency-management") version "1.1.7"
  kotlin("plugin.spring") version "2.3.0"
  id("com.expediagroup.graphql") version "9.0.0-alpha.8"
  id("org.openapi.generator") version "7.19.0"
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
  "build/generate-resources/main/src/main/kotlin"
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
}

tasks.named("compileKotlin") {
  dependsOn(graphqlGeneratePdlClient, graphqlGenerateNomClient, "openApiGenerate")
}

// Generer en markdown-fil med versjoner av viktige avhengigheter
// Før kjøring/regenerering, slett docks/versions.md hvis den finnes
// kjør ./gradlew generateVersionInfo
tasks.register("generateVersionInfo") {
  doLast {
    val javaVersion = java.toolchain.languageVersion.get().asInt()
    val gradleVersion = gradle.gradleVersion

    val versions = mapOf(
      "Java" to javaVersion,
      "Gradle" to gradleVersion,
      "Spring Boot" to springBootVersion,
      "Kotlin" to kotlinVersion,
      "Jackson" to jacksonVersion,
      "Coroutines" to coroutinesVersion,
      "Token Support" to tokenSupportVersion,
      "GraphQL Kotlin" to graphQLKotlinVersion
    )

    val content = versions.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }

    file("docs/versions.md").writeText(content)
  }
}


dependencies {
  // Jackson - BOM handles transitive versions
  implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

  implementation("org.apache.commons:commons-lang3") {
    version { strictly("3.18.0") }
    because("Fixes CVE-2025-48924")
  }

  implementation("tools.jackson.module:jackson-module-kotlin:3.0.4")

  // Tracing (Micrometer → OpenTelemetry)
  implementation("io.micrometer:micrometer-tracing-bridge-otel")
  runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")

  // Strukturerte JSON-logger (til stdout -> NAIS logger)
  implementation("net.logstash.logback:logstash-logback-encoder:9.0")

  implementation("io.projectreactor.netty:reactor-netty-http")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")

  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutinesVersion")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("io.mockk:mockk:1.14.9")
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
  implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
  implementation("no.nav.security:token-client-spring:$tokenSupportVersion")
  implementation(kotlin("stdlib"))
  implementation("com.expediagroup:graphql-kotlin-client-jackson:$graphQLKotlinVersion")
  implementation("com.expediagroup:graphql-kotlin-client:$graphQLKotlinVersion")
  implementation("com.expediagroup:graphql-kotlin-spring-client:$graphQLKotlinVersion")

  // Swagger UI og OpenAPI-dokumentasjon
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.42")
  implementation("io.swagger.core.v3:swagger-models:2.2.42")
}
