package soaringlizards.datalakesync.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.net.URI

@Configuration
class RestClientConfig {

    @Bean
    fun icebergRestClient(config: SparkSqlConfig): RestClient {
        return RestClient.builder()
            .baseUrl(config.catalog.uri)
            .defaultHeader("Content-Type", "application/json")
            .build()
    }
}