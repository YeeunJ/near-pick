plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":common"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.redisson:redisson:3.36.0")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")
    implementation("io.micrometer:micrometer-core")
    implementation("software.amazon.awssdk:s3:2.25.23")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> { useJUnitPlatform() }

tasks.jacocoTestReport {
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude(
                "**/entity/**",
                "**/repository/**",
                "**/mapper/**",
                "**/JpaConfig*",
            )
        }
    }))
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            includes = listOf("com.nearpick.nearpick.*.service.*")
            limit { minimum = "0.80".toBigDecimal() }
        }
    }
}
