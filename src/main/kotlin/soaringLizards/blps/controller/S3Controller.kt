package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.net.URI

@RestController
@RequestMapping("/api/s3")
@Tag(name = "S3")
class S3Controller(
    @Value("\${aws.s3.region}")
    private val region: String,
    @Value("\${aws.s3.endpoint}")
    private val s3Endpoint: String,
    @Value("\${aws.s3.tenant-id:}")
    private val tenantId: String,
    @Value("\${aws.s3.access-key}")
    private val accessKey: String,
    @Value("\${aws.s3.secret-key}")
    private val secretKey: String,
    @Value("\${aws.s3.bucket}")
    private val myBuck: String,
) {
    private val s3Client by lazy {
        S3Client
            .builder()
            .httpClient(ApacheHttpClient.create())
            .region(Region.of(region))
            .forcePathStyle(true)
            .endpointOverride(normalizeEndpoint(s3Endpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(normalizeAccessKey(accessKey, tenantId), secretKey),
                ),
            )
            .build()
    }

    @GetMapping("/{key}")
    fun getById(@PathVariable("key") key: String): ByteArray =
        s3Client.getObject(
            GetObjectRequest
                .builder()
                .bucket(myBuck)
                .key(key)
                .build(),
        )
            .readAllBytes()

    @Suppress("HttpUrlsUsage")
    private fun normalizeEndpoint(endpoint: String) =
        URI.create(
            if (endpoint.startsWith("http://") || endpoint.startsWith("https://"))
                endpoint
            else
                "https://$endpoint",
        ).let { URI(it.scheme, it.userInfo, it.host, it.port, null, null, null) }

    private fun normalizeAccessKey(accessKey: String, tenantId: String) =
        when {
            accessKey.contains(':') || accessKey.contains('.') -> accessKey
            tenantId.isBlank() -> accessKey
            else -> "$tenantId:$accessKey"
        }

}
