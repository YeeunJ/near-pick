# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`near-pick` is a Spring Boot 4.0.3 application written in Kotlin 2.2.21, targeting Java 17.
Multi-module Gradle project (Phase 2 convention applied).

- Entry point: `app/src/main/kotlin/com/nearpick/app/NearPickApplication.kt`
- Conventions: `CONVENTIONS.md`

## Module Structure

```
app/             — Controller, Spring Boot entry point (@SpringBootApplication)
common/          — Shared: ApiResponse, BusinessException, ErrorCode
domain/          — Service interfaces, pure domain models, DTOs (no JPA)
domain-nearpick/ — ServiceImpl, JpaRepository, @Entity, Mapper
```

Dependency flow: `app →(compile) domain →(compile) common`; `app →(runtimeOnly) domain-nearpick`

> `domain-nearpick` is `runtimeOnly` in `app` — importing its classes in `app` is a compile error by design.

## Package Roots

| Module | Root package |
|--------|--------------|
| `app` | `com.nearpick.app` |
| `common` | `com.nearpick.common` |
| `domain` | `com.nearpick.domain` |
| `domain-nearpick` | `com.nearpick.nearpick` |

## Build & Run Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew :app:bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.nearpick.app.NearPickApplicationTests"

# Clean build artifacts
./gradlew clean

# Build without running tests
./gradlew build -x test
```

## Architecture

Multi-module Spring Boot layered architecture using Kotlin. Kotlin compiler options enforce JSR-305 strict null safety (`-Xjsr305=strict`) and default annotation target to param-property (`-Xannotation-default-target=param-property`).

Tests use JUnit 5 via `useJUnitPlatform()`.
