plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"

    id("com.google.cloud.tools.jib") version "3.5.2"

    // id("org.liquibase.gradle") version "3.1.0"
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
            "-Xannotation-default-target=param-property"
        )
    }
    sourceSets {
        val main by getting {
            kotlin.srcDir("build/generated/main/kotlin")
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
        implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        //implementation("org.springframework.boot:spring-boot-starter-data-redis")
        //implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
        implementation("org.springframework.boot:spring-boot-starter-jdbc")
        implementation("org.springframework.boot:spring-boot-starter-jooq")
        implementation("org.springframework.boot:spring-boot-starter-liquibase")
        implementation("org.springframework.boot:spring-boot-starter-security")
        //implementation("org.springframework.boot:spring-boot-starter-session-data-redis")
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        implementation("org.springframework.boot:spring-boot-starter-webmvc")
        implementation("org.springframework.boot:spring-boot-starter-webservices")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("tools.jackson.module:jackson-module-kotlin")
        
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
        // testImplementation("org.springframework.boot:spring-boot-starter-data-redis-reactive-test")
        // testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
        testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
        testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
        testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
        testImplementation("org.springframework.boot:spring-boot-starter-security-test")
        // testImplementation("org.springframework.boot:spring-boot-starter-session-data-redis-test")
        testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
        testImplementation("org.springframework.boot:spring-boot-starter-webservices-test")
        testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
        testImplementation(enforcedPlatform("org.testcontainers:testcontainers-bom:1.19.3"))
        testImplementation("org.testcontainers:testcontainers") {
            version { strictly("1.19.3") }
        }
        testImplementation("org.testcontainers:junit-jupiter")
        testImplementation("org.testcontainers:postgresql")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    configurations.all {
        resolutionStrategy {
            force("org.testcontainers:testcontainers:1.19.3")
            force("org.testcontainers:jdbc:1.19.3")
            force("org.testcontainers:database-commons:1.19.3")
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
        // Docker Engine 29+ rejects old client API versions (e.g. 1.41). Testcontainers/docker-java reads this key.
        environment("api.version", "1.44")
        systemProperty("api.version", "1.44")

        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = false
        }
    }

// --- Env helpers ---
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

    val envProps: Map<String, String> = dotenv.ifEmpty { System.getenv() }
    val dbHost = envProps["POSTGRES_HOST"] ?: "localhost"
    val dbPort = envProps["POSTGRES_PORT"] ?: "5432"
    val dbName = envProps["POSTGRES_DB"] ?: "studs"
    val dbUser = envProps["POSTGRES_USER"] ?: ""
    val dbPassword = envProps["POSTGRES_PASSWORD"] ?: ""
    val dbSchema = envProps["POSTGRES_SCHEMA"] ?: "public"
    val jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"

    val liquibaseUpdate by tasks.register<JavaExec>("liquibaseUpdate") {
        group = "liquibase"
        description = "Apply database migrations before code generation"

        classpath = configurations["liquibaseRuntime"]
        mainClass.set("liquibase.integration.commandline.Main")

        args = listOf(
            "--changeLogFile=src/main/resources/db/db.changelog-master.yaml",
            "--url=$jdbcUrl",
            "--username=$dbUser",
            "--password=$dbPassword",
            "--defaultSchemaName=$dbSchema",
            "--liquibaseSchemaName=$dbSchema",
            "update"
        )
    }
    val liquibaseGenerateChangeLog by tasks.register<JavaExec>("liquibaseGenerateChangeLog") {
        group = "liquibase"
        description = "Generate SQL changelog from existing database schema"

        classpath = configurations["liquibaseRuntime"]
        mainClass.set("liquibase.integration.commandline.Main")
        isIgnoreExitValue = false

        val outputFile =
            project.file("src/main/resources/db/changelog/000-generated-initial.postgresql.sql").absolutePath

        doFirst {
            file(outputFile).parentFile.mkdirs()
            println("📁 Output: $outputFile")
        }

        // ✅ SWAPPED: Reference = your DB, URL = empty
        args = listOf(
            "--changeLogFile=$outputFile",
            // URL = empty database (where changes will be applied)
            "--url=jdbc:h2:mem:empty;DB_CLOSE_DELAY=-1",
            "--driver=org.h2.Driver",
            "--username=sa",
            "--password=",
            // Reference = your PostgreSQL (the schema to copy)
            "--referenceUrl=$jdbcUrl",
            "--referenceUsername=$dbUser",
            "--referencePassword=$dbPassword",
            "--referenceDefaultSchemaName=$dbSchema",
            "--logLevel=INFO",
            "diffChangelog"
        )

        doLast {
            val outFile = file(outputFile)
            if (!outFile.exists() || outFile.length() == 0L) {
                throw GradleException("❌ No SQL changelog generated at: $outputFile")
            }
            println("✅ Generated SQL changelog: ${outFile.length()} bytes")
        }
    }
    val liquibaseSnapshot by tasks.register<JavaExec>("liquibaseSnapshot") {
        group = "liquibase"
        description = "Debug: print database schema snapshot"

        classpath = configurations["liquibaseRuntime"]
        mainClass.set("liquibase.integration.commandline.Main")
        isIgnoreExitValue = false

        args = listOf(
            "--url=$jdbcUrl",
            "--username=$dbUser",
            "--password=$dbPassword",
            "--defaultSchemaName=$dbSchema",
            "--logLevel=INFO",
            "snapshot",
            "--snapshotFormat=yaml"
        )
    }

    tasks.register<JavaExec>("generateJooq") {
        group = "jooq"

        classpath = configurations["jooqGenerator"]
        mainClass.set("org.jooq.codegen.GenerationTool")
        args = listOf("src/main/resources/jooq-config.xml")

        outputs.upToDateWhen { false }

        val genEnv = envProps.filterKeys { it.startsWith("POSTGRES_") }.toMutableMap().apply {
            putIfAbsent("POSTGRES_HOST", dbHost)
            putIfAbsent("POSTGRES_PORT", dbPort)
            putIfAbsent("POSTGRES_DB", dbName)
            putIfAbsent("POSTGRES_USER", dbUser)
            putIfAbsent("POSTGRES_PASSWORD", dbPassword)
            putIfAbsent("POSTGRES_SCHEMA", dbSchema)
        }
        environment(genEnv)
        genEnv.forEach { (k, v) -> systemProperty(k, v) }
    }

    tasks.named("generateJooq") {
        dependsOn(liquibaseUpdate)
    }

    tasks.matching { it.name == "generateSources" }.configureEach {
        dependsOn(liquibaseUpdate)
    }
}
