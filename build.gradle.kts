import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	kotlin("jvm") version "2.3.0"
	kotlin("plugin.spring") version "2.3.0"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

group = "com.outboxpattern"
version = "0.0.1-SNAPSHOT"
description = "POC de demonstração do Outbox Pattern (Kotlin + Spring Boot + Mongo + Kafka)"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Web + REST + validação de entrada dos DTOs
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Persistência: Spring Data MongoDB (não JPA — ver CLAUDE.md, "Stack confirmada")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

	// Mensageria
	implementation("org.springframework.kafka:spring-kafka")

	// Observabilidade / healthcheck exigido pelo docker-compose (depends_on: service_healthy)
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// Resiliência no consumer Kafka (Circuit Breaker + Retry) — chega no MVP4,
	// dependência já fixada agora para não recalcular compatibilidade depois.
	implementation("io.github.resilience4j:resilience4j-spring-boot4:2.4.0")

	// Serialização Avro do payload publicado no tópico Kafka (schema em src/main/avro)
	implementation("org.apache.avro:avro:1.12.0")

	// Swagger UI — a liderança interage clicando "Try it out", sem Postman/curl
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

	// Kotlin
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Dependência padrão do Spring Initializr — mantida por convenção do ecossistema,
	// mas sem suíte de testes própria neste POC (decisão explícita do usuário).
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_21
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Classes geradas a partir dos .avsc em src/main/avro (build/generated-main-avro-java)
// entram automaticamente no sourceSet "main" via plugin — nenhuma config extra necessária.
