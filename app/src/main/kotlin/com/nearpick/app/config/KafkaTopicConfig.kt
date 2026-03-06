package com.nearpick.app.config

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
        val config = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "flash-purchase-cg",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "com.nearpick.*",
            JsonDeserializer.VALUE_DEFAULT_TYPE to
                "com.nearpick.nearpick.transaction.messaging.FlashPurchaseRequestEvent",
        )
        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
    ): ConcurrentKafkaListenerContainerFactory<String, Any> =
        ConcurrentKafkaListenerContainerFactory<String, Any>().also {
            it.setConsumerFactory(consumerFactory)
        }
}
