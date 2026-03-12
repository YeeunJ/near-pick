package com.nearpick.app.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class RedisConfig {

    /**
     * Jackson 2.x ObjectMapper — Redis 직렬화 전용.
     * Spring Boot 4.x 는 tools.jackson(3.x)을 기본 사용하므로 Redis용으로 별도 구성.
     * DefaultTyping.EVERYTHING: Kotlin data class(final)도 타입 메타데이터 포함.
     */
    @Bean(name = ["redisObjectMapper"])
    fun redisObjectMapper(): ObjectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.EVERYTHING,
            JsonTypeInfo.As.PROPERTY,
        )
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val serializer = GenericJackson2JsonRedisSerializer(redisObjectMapper())
        return RedisTemplate<String, Any>().apply {
            this.connectionFactory = connectionFactory
            keySerializer = StringRedisSerializer()
            valueSerializer = serializer
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = serializer
        }
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val jsonSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper())

        /**
         * products-nearby 캐시: PageImpl/PageRequest/Sort 는 Jackson 역직렬화 불가
         * (별도 @JsonCreator 없음). Page 계층 전체가 java.io.Serializable 이므로
         * JDK 직렬화를 사용해 문제 회피.
         */
        val jdkSerializer = JdkSerializationRedisSerializer()

        val jsonConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
            )
            .disableCachingNullValues()

        val nearbyConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jdkSerializer)
            )
            .disableCachingNullValues()
            .entryTtl(Duration.ofSeconds(30))

        val cacheConfigs = mapOf(
            "products-detail" to jsonConfig.entryTtl(Duration.ofSeconds(60)),
            "products-nearby" to nearbyConfig,
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(jsonConfig.entryTtl(Duration.ofMinutes(5)))
            .withInitialCacheConfigurations(cacheConfigs)
            .build()
    }

    @Bean
    fun redissonClient(
        @Value("\${spring.data.redis.host:localhost}") host: String,
        @Value("\${spring.data.redis.port:6379}") port: Int,
    ): RedissonClient {
        val config = Config()
        config.useSingleServer()
            .setAddress("redis://$host:$port")
            .setConnectionPoolSize(10)
            .setConnectionMinimumIdleSize(5)
        return Redisson.create(config)
    }
}
