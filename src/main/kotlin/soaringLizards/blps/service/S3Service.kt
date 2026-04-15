package soaringLizards.blps.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.net.URI
import java.time.Duration
import java.util.UUID

@Service
class S3Service(
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
    private val bucket: String,

    ) {

    private val creds = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(
            normalizeAccessKey(accessKey, tenantId),
            secretKey,
        ),
    )

    private val s3Config = S3Configuration.builder()
        .pathStyleAccessEnabled(true)
        .build()

    private val s3Client by lazy {
        S3Client.builder()
            .httpClient(ApacheHttpClient.create())
            .region(Region.of(region))
            .serviceConfiguration(s3Config)
            .endpointOverride(normalizeEndpoint(s3Endpoint))
            .credentialsProvider(creds)
            .build()
    }

    private val s3Presigner by lazy {
        S3Presigner.builder()
            .region(Region.of(region))
            .serviceConfiguration(s3Config)
            .endpointOverride(normalizeEndpoint(s3Endpoint))
            .credentialsProvider(creds)
            .build()
    }

    fun get(key: String) = s3Presigner
        .presignGetObject(
            GetObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(
                    GetObjectRequest
                        .builder()
                        .bucket(bucket)
                        .key(key)
                        .build(),
                )
                .build(),
        )
        .url()
        .toString()


    fun upload(file: MultipartFile) =
        s3Client
            .putObject(
                PutObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(UUID.randomUUID().toString() + ".png")
                    .build()!!,
                software.amazon.awssdk.core.sync.RequestBody
                    .fromInputStream(
                        file.inputStream,
                        file.size,
                    ),
            )

    fun delete(key: String): Unit = TODO()

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