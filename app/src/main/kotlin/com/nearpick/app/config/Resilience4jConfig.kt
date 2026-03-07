package com.nearpick.app.config

import org.springframework.context.annotation.Configuration

// Circuit Breaker 설정은 application.properties 선언형 설정으로 관리
// resilience4j.circuitbreaker.instances.flashPurchase.*
@Configuration
class Resilience4jConfig
