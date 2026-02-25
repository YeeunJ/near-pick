package com.nearpick.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * dev 프로필 전용 H2 콘솔 보안 설정.
 * 운영 환경에서는 이 설정이 로드되지 않아 H2 콘솔에 접근할 수 없다.
 */
@Configuration
@Profile("dev")
class DevSecurityConfig {

    @Bean
    @Order(1)
    fun h2ConsoleFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/h2-console/**")
            .csrf { it.disable() }
            .headers { it.frameOptions { fo -> fo.disable() } }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
        return http.build()
    }
}
