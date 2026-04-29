package soaringLizards.blps.service

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import soaringLizards.blps.config.properties.S3Properties
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.apache.ProxyConfiguration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.net.URI
import java.time.Duration
import java.util.UUID

@Service
class S3Service(
    val s3Properties: S3Properties,
) {
    companion object Util {
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

    private val creds = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(
            normalizeAccessKey(s3Properties.accessKey, s3Properties.tenantId),
            s3Properties.secretKey,
        ),
    )

    private val s3Config = S3Configuration.builder()
        .pathStyleAccessEnabled(true)
        .build()

    private val s3Client by lazy {
        S3Client.builder()
            .httpClient(httpClient)
            .region(Region.of(s3Properties.region))
            .serviceConfiguration(s3Config)
            .endpointOverride(normalizeEndpoint(s3Properties.endpoint))
            .credentialsProvider(creds)
            .build()
    }

    private val httpClient by lazy {
        ApacheHttpClient.builder()
            .proxyConfiguration(
                ProxyConfiguration.builder()
                    .useSystemPropertyValues(false)
                    .useEnvironmentVariableValues(false)
                    .build(),
            )
            .build()
    }

    private val s3Presigner by lazy {
        S3Presigner.builder()
            .region(Region.of(s3Properties.region))
            .serviceConfiguration(s3Config)
            .endpointOverride(normalizeEndpoint(s3Properties.endpoint))
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
                        .bucket(s3Properties.bucket)
                        .key(key)
                        .build(),
                )
                .build(),
        )
        .url()
        .toString()

    fun upload(file: MultipartFile): String {
        val key = buildObjectKey("uploads", file.originalFilename)
        upload(key, file)
        return key
    }

    fun uploadAchievementIcon(file: MultipartFile): String {
        val key = buildObjectKey("achievements", file.originalFilename)
        upload(key, file)
        return key
    }

    fun delete(key: String) {
        s3Client
            .deleteObject(
                DeleteObjectRequest
                    .builder()
                    .bucket(s3Properties.bucket)
                    .key(key)
                    .build(),
            )
    }

    private fun upload(key: String, file: MultipartFile) =
        s3Client.putObject(
            createPutObjectRequest(key, file),
            RequestBody.fromInputStream(file.inputStream, file.size),
        )

    private fun buildObjectKey(prefix: String, originalFilename: String?): String {
        val sanitizedFilename = originalFilename
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
            ?.takeIf { it.isNotBlank() }
            ?: "file.bin"

        return "$prefix/${UUID.randomUUID()}-$sanitizedFilename"
    }

    private fun createPutObjectRequest(key: String, file: MultipartFile) =
        PutObjectRequest
            .builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .contentType(file.contentType)
            .build()!!
}
