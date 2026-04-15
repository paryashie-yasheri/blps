package soaringLizards.blps.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "aws.s3")
@Component
data class S3Properties(
    val region: String = "",
    val endpoint: String = "",
    val tenantId: String = "",
    val accessKey: String = "",
    val secretKey: String = "",
    val bucket: String = "",
)