package com.soaringlizards.datalakesync

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Import(MockSparkConfig::class)
@SpringBootTest
@ActiveProfiles("test")
class DatalakeSyncApplicationTests {

    @Test
    fun contextLoads() {
    }

}