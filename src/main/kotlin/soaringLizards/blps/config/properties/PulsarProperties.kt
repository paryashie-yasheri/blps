package soaringLizards.blps.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.pulsar")
data class PulsarProperties(
    var enabled: Boolean = true,
    var serviceUrl: String = "pulsar://localhost:6650",
    var rewardRedemptionTopic: String = "persistent://public/default/reward-redemptions",
    var producerName: String = "blps-reward-redemptions",
    var sendTimeoutMs: Long = 3_000,
)
