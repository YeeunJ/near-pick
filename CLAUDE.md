# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`near-pick` is a Spring Boot 4.0.3 application written in Kotlin 2.2.21, targeting Java 17. Currently in early scaffolding stage.

- Root package: `com.nearpick.app`
- Entry point: `src/main/kotlin/com/nearpick/app/NearPickApplication.kt`

## Build & Run Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

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

Standard Spring Boot layered architecture using Kotlin. Kotlin compiler options enforce JSR-305 strict null safety (`-Xjsr305=strict`) and default annotation target to param-property (`-Xannotation-default-target=param-property`).

Tests use JUnit 5 via `useJUnitPlatform()`.
