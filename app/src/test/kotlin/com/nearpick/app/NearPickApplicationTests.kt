package com.nearpick.app

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class NearPickApplicationTests {

    @Test
    fun `애플리케이션 컨텍스트가 정상 로딩된다`() {
        // Spring Boot context loads without error
    }
}
