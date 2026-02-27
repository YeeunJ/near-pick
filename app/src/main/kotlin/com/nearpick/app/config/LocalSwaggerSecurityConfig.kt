package com.nearpick.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@Profile("local")
class LocalSwaggerSecurityConfig {

    @Bean
    @Order(1)
    fun swaggerFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .csrf { it.disable() }
        return http.build()
    }
}
