package com.nearpick.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val rateLimitFilter: RateLimitFilter,
    private val corsConfigurationSource: CorsConfigurationSource,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource) }
            .csrf { it.disable() }
            .headers { headers ->
                headers
                    .frameOptions { it.deny() }
                    .contentTypeOptions { }
                    .httpStrictTransportSecurity { hsts ->
                        hsts.includeSubDomains(true).maxAgeInSeconds(31536000)
                    }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // 인증 없이 허용 (Auth 엔드포인트, 공개 상품 조회)
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/products/nearby").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/products/{productId}").permitAll()
                    // 소비자 전용
                    .requestMatchers("/api/wishlists/**").hasRole("CONSUMER")
                    .requestMatchers(HttpMethod.POST, "/api/reservations").hasRole("CONSUMER")
                    .requestMatchers(HttpMethod.GET, "/api/reservations/me").hasRole("CONSUMER")
                    .requestMatchers(HttpMethod.PATCH, "/api/reservations/{id}/cancel").hasRole("CONSUMER")
                    .requestMatchers("/api/flash-purchases/**").hasRole("CONSUMER")
                    // 소상공인 전용
                    .requestMatchers(HttpMethod.POST, "/api/products").hasRole("MERCHANT")
                    .requestMatchers(HttpMethod.GET, "/api/products/me").hasRole("MERCHANT")
                    .requestMatchers(HttpMethod.PATCH, "/api/products/{productId}/close").hasRole("MERCHANT")
                    .requestMatchers("/api/merchants/**").hasRole("MERCHANT")
                    .requestMatchers(HttpMethod.GET, "/api/reservations/merchant").hasRole("MERCHANT")
                    .requestMatchers(HttpMethod.PATCH, "/api/reservations/{id}/confirm").hasRole("MERCHANT")
                    // 관리자 전용
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    // 나머지 인증 필요
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
