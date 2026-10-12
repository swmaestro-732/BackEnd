package com.example.backend.bootstrap.place

import com.example.backend.bootstrap.config.GooglePlacesProperties
import com.example.backend.place.application.port.inbound.PlaceBusinessHoursSyncUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** 부팅 백필 러너 — 플래그·키 조건과 백그라운드 실행, 예외 삼킴(fail-soft)을 검증한다. */
class PlaceBusinessHoursBackfillRunnerTest {
    private val started = CountDownLatch(1)

    private fun useCase(onBackfill: () -> Int) =
        object : PlaceBusinessHoursSyncUseCase {
            override fun sync(placeIds: List<Long>): Int = 0

            override fun backfillAll(): Int {
                started.countDown()
                return onBackfill()
            }
        }

    private fun run(
        properties: GooglePlacesProperties,
        onBackfill: () -> Int = { 3 },
    ) = PlaceBusinessHoursBackfillRunner(properties, useCase(onBackfill)).run(DefaultApplicationArguments())

    @Test
    fun `플래그가 켜지고 키가 있으면 백필을 실행한다`() {
        run(GooglePlacesProperties(apiKey = "api-key", backfillOnStartup = true))

        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun `백필이 실패해도 예외를 밖으로 던지지 않는다`() {
        run(GooglePlacesProperties(apiKey = "api-key", backfillOnStartup = true)) { error("백필 실패 모사") }

        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun `플래그가 꺼져 있으면 실행하지 않는다`() {
        run(GooglePlacesProperties(apiKey = "api-key", backfillOnStartup = false))

        assertThat(started.await(200, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun `키가 비면 실행하지 않는다`() {
        run(GooglePlacesProperties(apiKey = "", backfillOnStartup = true))

        assertThat(started.await(200, TimeUnit.MILLISECONDS)).isFalse()
    }
}
