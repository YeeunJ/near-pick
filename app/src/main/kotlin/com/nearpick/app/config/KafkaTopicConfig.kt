package com.nearpick.app.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
@EnableKafka
class KafkaTopicConfig {

    // ── Topics ────────────────────────────────────────────────────────────────

    @Bean
    fun flashPurchaseRequestTopic(): NewTopic =
        TopicBuilder.name("flash-purchase-requests")
            .partitions(10)
            .replicas(1)
            .build()

    @Bean
    fun flashPurchaseDlqTopic(): NewTopic =
        TopicBuilder.name("flash-purchase-dlq")
            .partitions(1)
            .replicas(1)
            .build()

    // ── Producer ──────────────────────────────────────────────────────────────

    @Bean
    fun producerFactory(
        @Value("\${spring.kafka.bootstrap-servers:localhost:9092}") bootstrapServers: String,
    ): ProducerFactory<String, Any> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
        )
        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> =
        KafkaTemplate(producerFactory)

    // ── Consumer ──────────────────────────────────────────────────────────────

    @Bean
    fun consumerFactory(
        @Value("\${spring.kafka.bootstrap-servers:localhost:9092}") bootstrapServers: String,
    ): ConsumerFactory<String, Any> {
        val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        val valueDeserializer = JsonDeserializer<Any>(objectMapper)
        valueDeserializer.configure(
            mapOf(
                JsonDeserializer.TRUSTED_PACKAGES to "com.nearpick.*",
                JsonDeserializer.VALUE_DEFAULT_TYPE to
                    "com.nearpick.nearpick.transaction.messaging.FlashPurchaseRequestEvent",
            ),
            false,
        )
        val config = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "flash-purchase-cg",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        )
        return DefaultKafkaConsumerFactory(config, StringDeserializer(), valueDeserializer)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
    ): ConcurrentKafkaListenerContainerFactory<String, Any> =
        ConcurrentKafkaListenerContainerFactory<String, Any>().also {
            it.setConsumerFactory(consumerFactory)
        }
}
