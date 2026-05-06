package soaringLizards.blps.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import soaringLizards.blps.config.properties.S3Properties
import java.net.URI

class S3ServiceTest {
    private val s3Service = S3Service(
        S3Properties(
            region = "ru-central-1",
            endpoint = "https://s3.cloud.ru",
            accessKey = "test-access-key",
            secretKey = "test-secret-key",
            bucket = "achievement-blps-bucket",
        ),
    )

    @Test
    fun `get normalizes object key before presigning url`() {
        val url = s3Service.get("rewards//icons/tung_tung.png")

        assertThat(URI.create(url).path)
            .isEqualTo("/achievement-blps-bucket/rewards/icons/tung_tung.png")
    }
}
