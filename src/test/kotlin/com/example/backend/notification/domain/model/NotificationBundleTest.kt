package com.example.backend.notification.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration
import java.time.Instant

class NotificationBundleTest {
    @Test
    fun `묶음 창은 10분이다`() {
        assertThat(NotificationBundle.BUNDLE_WINDOW).isEqualTo(Duration.ofMinutes(10))
    }

    @ParameterizedTest
    @CsvSource(
        "2026-10-24T09:59:59.999Z, 2026-10-24T09:50:00Z",
        "2026-10-24T10:00:00Z, 2026-10-24T10:00:00Z",
        "2026-10-24T10:09:59.999999999Z, 2026-10-24T10:00:00Z",
        "2026-10-24T10:10:00Z, 2026-10-24T10:10:00Z",
        "2026-10-24T23:59:59.999Z, 2026-10-24T23:50:00Z",
    )
    fun `발생 시간을 UTC 10분 창 시작으로 절삭한다`(
        occurredAt: String,
        expected: String,
    ) {
        assertThat(NotificationBundle.windowStart(Instant.parse(occurredAt))).isEqualTo(Instant.parse(expected))
    }
}
