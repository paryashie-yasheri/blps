package soaringLizards.blps.config

import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import soaringLizards.blps.config.properties.PulsarProperties
import java.util.concurrent.TimeUnit

@Configuration
@EnableConfigurationProperties(PulsarProperties::class)
class PulsarConfig(
    private val properties: PulsarProperties,
) {
    @Bean(destroyMethod = "close")
    @Lazy
    @ConditionalOnProperty(prefix = "app.pulsar", name = ["enabled"], havingValue = "true", matchIfMissing = true)
    fun pulsarClient(): PulsarClient =
        PulsarClient.builder()
            .serviceUrl(properties.serviceUrl)
            .build()

    @Bean(destroyMethod = "close")
    @Lazy
    @ConditionalOnBean(PulsarClient::class)
    fun rewardRedemptionProducer(pulsarClient: PulsarClient): Producer<ByteArray> =
        pulsarClient.newProducer(Schema.BYTES)
            .topic(properties.rewardRedemptionTopic)
            .producerName(properties.producerName)
            .sendTimeout(properties.sendTimeoutMs.toInt(), TimeUnit.MILLISECONDS)
            .create()

    @Bean(destroyMethod = "shutdown")
    fun rewardRedemptionEventExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 1
            maxPoolSize = 2
            queueCapacity = 100
            setThreadNamePrefix("reward-events-")
            initialize()
        }
}
