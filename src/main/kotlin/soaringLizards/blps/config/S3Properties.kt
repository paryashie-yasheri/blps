package soaringLizards.blps.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aws.s3")
data class S3Properties(
    val region: String = "",
    val endpoint: String = "",
    val tenantId: String = "",
    val accessKey: String = "",
    val secretKey: String = "",
    val bucket: String = "",
)
