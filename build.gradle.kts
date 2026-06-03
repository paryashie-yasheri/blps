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

fun stopTestContainerManager() {
    testContainerManagerInstance?.stop()
    testContainerManagerInstance = null
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
        image = "soaring-lizards/blps-main:jib-dev"
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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
val pulsarVersion = "4.0.10"
val camundaVersion = "7.23.0"

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
    // springboot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webservices")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.security:spring-security-oauth2-authorization-server:7.0.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
    implementation("jakarta.transaction:jakarta.transaction-api:2.0.1")
    implementation("com.atomikos:transactions-jta:6.0.0:jakarta")
    implementation("com.atomikos:transactions-jdbc:6.0.0:jakarta")
    implementation("org.postgresql:postgresql")

    // db & tests
    testImplementation(enforcedPlatform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))
    testImplementation("org.testcontainers:testcontainers") {
        version { strictly(testcontainersVersion) }
    }
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    add("jooqGenerator", "org.jooq:jooq-codegen")
    add("jooqGenerator", "org.jooq:jooq-meta")
    add("jooqGenerator", "org.postgresql:postgresql")
    add("liquibaseRuntime", "org.liquibase:liquibase-core")
    add("liquibaseRuntime", "org.postgresql:postgresql")
    add("liquibaseRuntime", "info.picocli:picocli:4.7.6")
    add("liquibaseRuntime", "com.h2database:h2:2.2.224") // точн надо?
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")


    // camunda
    implementation(platform("org.camunda.bpm:camunda-bom:$camundaVersion"))
    implementation("org.camunda.bpm:camunda-engine-spring-6")
    implementation("org.camunda.bpm:camunda-engine-plugin-spin")
    implementation("org.camunda.spin:camunda-spin-dataformat-all")

    // security
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    implementation("org.keycloak:keycloak-admin-client:26.0.9")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")


    // s3
    implementation("software.amazon.awssdk:s3:2.42.33")
    implementation("software.amazon.awssdk:apache-client:2.42.33")

    // other
    implementation("org.slf4j:jcl-over-slf4j")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation(enforcedPlatform("org.apache.pulsar:pulsar-bom:$pulsarVersion"))
    implementation("org.apache.pulsar:pulsar-client")

}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.apache.pulsar") {
                useVersion(pulsarVersion)
                because("Keep Pulsar client modules aligned with the broker image used in dev compose")
            }
        }
        force("org.testcontainers:testcontainers:$testcontainersVersion")
        force("org.testcontainers:jdbc:$testcontainersVersion")
        force("org.testcontainers:database-commons:$testcontainersVersion")
        force("org.apache.commons:commons-text:1.12.0")
        force("org.apache.commons:commons-lang3:3.12.0")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Test>("test") {
    environment("SPRING_PROFILES_ACTIVE", "test")
    environment("api.version", "1.44")
    systemProperty("spring.profiles.active", "test")
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

fun createEnvMap() = dotenv.ifEmpty { System.getenv() }!!

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
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    classpath = configurations["liquibaseRuntime"]
    mainClass.set("liquibase.integration.commandline.Main")
    doFirst {

        val connectionArgs = createLiquibaseArgs(createEnvMap()).toMutableList()
        connectionArgs += "update"

        args = connectionArgs
    }
}

val liquibaseUpdateTestContainer by tasks.register<JavaExec>("liquibaseUpdateTestContainer") {
    group = "liquibase"

    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    doNotTrackState("Liquibase uses Testcontainers/Docker external state")

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
        stopTestContainerManager()
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
        stopTestContainerManager()
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

val codegen by tasks.register<DefaultTask>("codegen") {
    doNotTrackState("codegen depends on external DB/Testcontainers/server")
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    dependsOn(
        generateJooq,
    )
}

// fixme: попытался починить что у меня переодически после кодгена не останавливаются контейнеры, но эти штуки депрекейтед
gradle.taskGraph.addTaskExecutionListener(
    object : TaskExecutionListener {
        override fun beforeExecute(task: Task) = Unit

        override fun afterExecute(task: Task, state: TaskState) {
            if (state.failure != null) {
                println("Task ${task.path} failed, stopping Testcontainers...")
                stopTestContainerManager()
            }
        }
    },
)
