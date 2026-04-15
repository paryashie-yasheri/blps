import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

val DEFAULT_POSTGRES_IMAGE = "postgres:16.6-alpine"
val DEFAULT_POSTGRES_DB = "study"
val DEFAULT_POSTGRES_USER = "test"
val DEFAULT_POSTGRES_PASSWORD = "test"
val DEFAULT_POSTGRES_SCHEMA = "public"

fun Map<String, String>.nonBlank(key: String): String? = this[key]?.takeIf { it.isNotBlank() }

data class TestContainerConfig(
    val imageName: String = DEFAULT_POSTGRES_IMAGE,
    val databaseName: String = DEFAULT_POSTGRES_DB,
    val dbUsername: String = DEFAULT_POSTGRES_USER,
    val dbPassword: String = DEFAULT_POSTGRES_PASSWORD,
    val schemaName: String = DEFAULT_POSTGRES_SCHEMA,
)

fun testContainerConfigFromEnv(env: Map<String, String>): TestContainerConfig {
    val defaults = TestContainerConfig()

    return defaults.copy(
        imageName = env.nonBlank("POSTGRES_IMAGE") ?: defaults.imageName,
        databaseName = env.nonBlank("POSTGRES_DB") ?: defaults.databaseName,
        dbUsername = env.nonBlank("POSTGRES_USER") ?: defaults.dbUsername,
        dbPassword = env.nonBlank("POSTGRES_PASSWORD") ?: defaults.dbPassword,
        schemaName = env.nonBlank("POSTGRES_SCHEMA") ?: defaults.schemaName,
    )
}

var testContainerManagerInstance: TestContainerManager? = null

fun getOrCreateTestContainerManager(env: Map<String, String>): TestContainerManager {
    if (testContainerManagerInstance == null) {
        testContainerManagerInstance = TestContainerManager(testContainerConfigFromEnv(env))
    }
    testContainerManagerInstance!!.start()
    return testContainerManagerInstance!!
}

class TestContainerManager(
    private val config: TestContainerConfig = TestContainerConfig(),
) {
    private var container: PostgreSQLContainer<*>? = null

    fun start(): PostgreSQLContainer<*> {
        if (container == null) {
            println("Starting Testcontainers with image: ${config.imageName}")
            try {
                container =
                    PostgreSQLContainer<Nothing>(
                        DockerImageName.parse(config.imageName).asCompatibleSubstituteFor("postgres"),
                    )
                        .apply {
                            withDatabaseName(config.databaseName)
                            withUsername(config.dbUsername)
                            withPassword(config.dbPassword)
                            withExposedPorts(5432)
                            start()
                        }
                ensureSchemaExists(container!!)
                println("Container started: ${container?.containerId?.substring(0, 12)}")
            } catch (e: Exception) {
                println("Failed to start container: ${e.message}")
                throw e
            }
        }
        return container!!
    }

    private fun ensureSchemaExists(container: PostgreSQLContainer<*>) {
        if (config.schemaName.equals("public", ignoreCase = true))
            return


        val escapedSchema = config.schemaName.replace("\"", "\"\"")
        val escapedUser = config.dbUsername.replace("\"", "\"\"")

        container.createConnection("").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("""CREATE SCHEMA IF NOT EXISTS "$escapedSchema" AUTHORIZATION "$escapedUser"""")
            }
        }
    }

    fun toPostgresEnv(): Map<String, String> {
        val c = container ?: throw IllegalStateException("Container not started")
        return mapOf(
            "POSTGRES_HOST" to c.host,
            "POSTGRES_PORT" to c.firstMappedPort.toString(),
            "POSTGRES_DB" to c.databaseName,
            "POSTGRES_USER" to c.username,
            "POSTGRES_PASSWORD" to c.password,
            "POSTGRES_SCHEMA" to config.schemaName,
        )
    }

    fun stop() {
        println("Stopping container...")
        container?.stop()
        container = null
    }
}

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"

    id("com.google.cloud.tools.jib") version "3.5.2"
    id("org.openapi.generator") version "7.9.0"
}

group = "soaringLizards"
version = "0.0.1-SNAPSHOT"
description = "blps"

jib {
    to {
        image = "blps/backend:jib-dev"
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
        )
    }
    sourceSets {
        @Suppress("unused") val main by getting {
            kotlin.srcDir("build/generated/main/kotlin")
            kotlin.srcDir("build/generated/edx-models")
        }
    }
}

val testcontainersVersion = "1.21.4"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.postgresql:postgresql:42.5.0")
        classpath("org.testcontainers:postgresql:1.21.4")
        classpath("org.jooq:jooq-codegen:3.18.7")
    }
}

repositories {
    mavenCentral()
}

configurations {
    create("jooqGenerator")
    create("liquibaseRuntime")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webservices")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    implementation("org.slf4j:jcl-over-slf4j")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    add("jooqGenerator", "org.jooq:jooq-codegen")
    add("jooqGenerator", "org.jooq:jooq-meta")
    add("jooqGenerator", "org.postgresql:postgresql")
    add("liquibaseRuntime", "org.liquibase:liquibase-core")
    add("liquibaseRuntime", "org.postgresql:postgresql")
    add("liquibaseRuntime", "info.picocli:picocli:4.7.6")
    add("liquibaseRuntime", "com.h2database:h2:2.2.224")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webservices-test")
    testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation(enforcedPlatform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))
    testImplementation("org.testcontainers:testcontainers") {
        version { strictly(testcontainersVersion) }
    }
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.all {
    resolutionStrategy {
        force("org.testcontainers:testcontainers:$testcontainersVersion")
        force("org.testcontainers:jdbc:$testcontainersVersion")
        force("org.testcontainers:database-commons:$testcontainersVersion")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}


tasks.test {
    environment("api.version", "1.44")
    systemProperty("api.version", "1.44")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

val dotenv: Map<String, String> = run {
    val envFile = rootProject.file(".env")
    if (!envFile.exists()) emptyMap() else envFile.readLines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) null
            else trimmed.substringBefore("=").trim() to trimmed.substringAfter("=").trim().trim('"', '\'')
        }
        .toMap()
}

fun createEnvMap(): Map<String, String> {
    return dotenv.ifEmpty { System.getenv() }
}

fun createLiquibaseArgs(env: Map<String, String>): List<String> {
    val url = "jdbc:postgresql://${env["POSTGRES_HOST"]}:${env["POSTGRES_PORT"]}/${env["POSTGRES_DB"]}"

    return mutableListOf(
        "--changeLogFile=src/main/resources/db/db.changelog-master.yaml",
        "--url=$url",
        "--username=${env["POSTGRES_USER"]}",
        "--password=${env["POSTGRES_PASSWORD"]}",
        "--defaultSchemaName=${env["POSTGRES_SCHEMA"]}",
        "--liquibaseSchemaName=${env["POSTGRES_SCHEMA"]}",
    )
}

val liquibaseUpdate by tasks.register<JavaExec>("liquibaseUpdate") {
    group = "liquibase"
    description = "Apply database migrations before code generation"

    classpath = configurations["liquibaseRuntime"]
    mainClass.set("liquibase.integration.commandline.LiquibaseCommandLine")

    val connectionArgs = createLiquibaseArgs(createEnvMap()).toMutableList()
    connectionArgs += "update"

    args = connectionArgs
}


val liquibaseUpdateTestContainer by tasks.register<JavaExec>("liquibaseUpdateTestContainer") {
    group = "liquibase"
    description = "Apply database migrations before code generation"
    classpath = configurations["liquibaseRuntime"]
    mainClass.set("liquibase.integration.commandline.Main")

    doFirst {

        val manager = getOrCreateTestContainerManager(createEnvMap())

        val connectionArgs = createLiquibaseArgs(manager.toPostgresEnv()).toMutableList()
        connectionArgs += "update"

        args = connectionArgs
    }
}

val generateJooq = tasks.register<JavaExec>("generateJooq") {
    group = "jooq"
    classpath = configurations["jooqGenerator"]
    mainClass.set("org.jooq.codegen.GenerationTool")
    args = listOf("src/main/resources/jooq-config.xml")
    outputs.upToDateWhen { false }

    doFirst {
        val manager = getOrCreateTestContainerManager(createEnvMap())
        val pgConfig = manager.toPostgresEnv()

        environment(pgConfig)
        pgConfig.forEach { (k, v) -> systemProperty(k, v) }
    }

    doLast {
        testContainerManagerInstance?.stop()
    }
}

tasks.register<JavaExec>("generateJooqFromExistingDb") {
    group = "jooq"
    classpath = configurations["jooqGenerator"]
    mainClass.set("org.jooq.codegen.GenerationTool")
    args = listOf("src/main/resources/jooq-config.xml")
    outputs.upToDateWhen { false }

    doFirst {
        val genEnv = createEnvMap()
        environment(createEnvMap())
        genEnv.forEach { (k, v) -> systemProperty(k, v) }
    }

    doLast {
        testContainerManagerInstance?.stop()
    }
}

@DisableCachingByDefault
tasks.named("generateJooq").configure {
    dependsOn(liquibaseUpdateTestContainer)
}

tasks.named("generateJooqFromExistingDb").configure {
    dependsOn(liquibaseUpdate)
}

tasks.matching { it.name == "generateSources" }.configureEach {
    dependsOn(liquibaseUpdate)
}

val downloadOpenApiSpec by tasks.register<DefaultTask>("downloadOpenApiSpec") {
    group = "openapi"

    outputs.file(layout.buildDirectory.dir("generated/openapi/openapi.json"))

    doLast {
        try {
            val outputDir = file(layout.buildDirectory.dir("generated/openapi").get().asFile)
            outputDir.mkdirs()
            val outputFile = file("${outputDir.absolutePath}/openapi.json")
            val localSpec = file("src/main/resources/openapi/openapi.json")
            if (localSpec.exists()) {
                localSpec.copyTo(outputFile, overwrite = true)
            } else {
                ant.withGroovyBuilder {
                    @Suppress("HttpUrlsUsage")
                    "get"(
                        "src" to "http://local.openedx.io/swagger.json",
                        "dest" to outputFile.absolutePath,
                        "usetimestamp" to "true",
                        "verbose" to "false",
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

val downloadOpenApiGeneratorCli by tasks.register<DefaultTask>("downloadOpenApiGeneratorCli") {
    group = "openapi"
    outputs.file(layout.buildDirectory.dir("openapi-generator-cli/openapi-generator-cli.jar"))

    doLast {
        val outputDir = file(layout.buildDirectory.dir("openapi-generator-cli").get().asFile)
        outputDir.mkdirs()
        val jarFile = file("${outputDir.absolutePath}/openapi-generator-cli.jar")

        if (!jarFile.exists()) {
            ant.withGroovyBuilder {
                "get"(
                    "src" to "https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/7.9.0/openapi-generator-cli-7.9.0.jar",
                    "dest" to jarFile.absolutePath,
                    "usetimestamp" to "true",
                    "verbose" to "false",
                )
            }
            println("Downloaded OpenAPI Generator CLI to: ${jarFile.absolutePath}")
        } else {
            println("OpenAPI Generator CLI already exists: ${jarFile.absolutePath}")
        }
    }
}

val generateEdxModels = tasks.register<Exec>("generateEdxModels") {
    group = "openapi"
    description = "Generate Kotlin models for Open edX API"
    dependsOn(downloadOpenApiSpec, downloadOpenApiGeneratorCli)

    val outputDir = layout.buildDirectory.dir("generated/edx-models").get().asFile

    doFirst {
        delete(outputDir)
        outputDir.mkdirs()
    }

    commandLine(
        "java", "-jar",
        "${rootProject.layout.buildDirectory.get().asFile}/openapi-generator-cli/openapi-generator-cli.jar",
        "generate",
        "-g", "kotlin",
        "-i", "${layout.buildDirectory.get().asFile}/generated/openapi/openapi.json",
        "-o", outputDir.absolutePath,
        "--skip-validate-spec",
        "--global-property", "models,modelDocs=false,modelTests=false",
//        "--openapi-generator-ignore-list",
//        "README.md,build.gradle,build.gradle.kts,settings.gradle,settings.gradle.kts,gradle.properties,gradlew,gradlew.bat,docs/**,src/test/**,gradle/**,.openapi-generator/**,.openapi-generator-ignore",
        "-p", "serializationLibrary=jackson",
        "-p", "sourceFolder=.",
//        "-p", "useBeanValidation=true",
        "-p", "enumPropertyNaming=PascalCase",
        "--model-package=soaringLizards.blps.edx.model",
    )
}

val codegen by tasks.register<DefaultTask>("codegen") {
    dependsOn(
        generateJooq,
        downloadOpenApiSpec,
        generateEdxModels,
    )
}
