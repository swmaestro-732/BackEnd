package com.example.backend.bootstrap.config

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration

class PushPropertiesTest {
    @Test
    fun `안드로이드 TTL 이 음수면 기동 시점에 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) { PushProperties(androidTtl = Duration.ofSeconds(-1)) }
    }
}
