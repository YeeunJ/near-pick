package com.nearpick.app.config

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RateLimitConfig {

    @Bean
    fun rateLimitProxyManager(
        @Value("\${spring.data.redis.host:localhost}") host: String,
        @Value("\${spring.data.redis.port:6379}") port: Int,
    ): LettuceBasedProxyManager<String> {
        val client = RedisClient.create("redis://$host:$port")
        val codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        val connection = client.connect(codec)
        return LettuceBasedProxyManager.builderFor(connection).build()
    }

    @Bean
    fun bucketProvider(proxyManager: LettuceBasedProxyManager<String>): BucketProvider =
        BucketProvider { key, config -> proxyManager.builder().build(key) { config } }
}
