package soaringlizards.datalakesync.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {

    @Bean
    fun icebergRestClient(config: SparkSqlConfig): RestClient {
        val baseUrl = config.catalog.uri.removeSuffix("/")
        return RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", "application/json")
            .build()
    }
}
