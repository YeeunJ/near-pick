package com.nearpick.app.config

import io.github.bucket4j.Bandwidth
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class RateLimitConfig {

    @Bean
    fun loginBandwidth(): Bandwidth =
        Bandwidth.builder()
            .capacity(10)
            .refillGreedy(10, Duration.ofMinutes(1))
            .build()

    @Bean
    fun apiBandwidth(): Bandwidth =
        Bandwidth.builder()
            .capacity(200)
            .refillGreedy(200, Duration.ofMinutes(1))
            .build()
}
