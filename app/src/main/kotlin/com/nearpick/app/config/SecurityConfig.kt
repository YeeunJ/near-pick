package com.nearpick.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // 인증 없이 허용
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/products/nearby", "/products/{id}").permitAll()
                    // 소비자 전용
                    .requestMatchers("/wishlists/**").hasRole("CONSUMER")
                    .requestMatchers(HttpMethod.POST, "/reservations").hasRole("CONSUMER")
                    .requestMatchers(HttpMethod.GET, "/reservations/me").hasRole("CONSUMER")
                    .requestMatchers(HttpMethod.PATCH, "/reservations/*/cancel").hasRole("CONSUMER")
                    .requestMatchers("/flash-purchases/**").hasRole("CONSUMER")
                    // 소상공인 전용
                    .requestMatchers(HttpMethod.POST, "/products").hasRole("MERCHANT")
                    .requestMatchers(HttpMethod.GET, "/products/me").hasRole("MERCHANT")
                    .requestMatchers(HttpMethod.PATCH, "/products/*/close").hasRole("MERCHANT")
                    .requestMatchers("/merchants/**").hasRole("MERCHANT")
                    .requestMatchers(HttpMethod.GET, "/reservations/merchant").hasRole("MERCHANT")
                    .requestMatchers(HttpMethod.PATCH, "/reservations/*/confirm").hasRole("MERCHANT")
                    // 관리자 전용
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    // 나머지 인증 필요
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
