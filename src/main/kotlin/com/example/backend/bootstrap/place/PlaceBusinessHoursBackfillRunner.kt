package com.example.backend.bootstrap.place

import com.example.backend.bootstrap.config.GooglePlacesProperties
import com.example.backend.place.application.port.inbound.PlaceBusinessHoursSyncUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import kotlin.concurrent.thread

/**
 * google.places.backfill-on-startup=true 면 부팅 후 영업시간 미동기화 장소를 백그라운드 스레드에서 채운다.
 * 장소당 외부 호출 두 번이라 기동(헬스체크)을 막지 않도록 러너 스레드에서 돌리지 않는다. 키가 비면 no-op.
 */
@Component
class PlaceBusinessHoursBackfillRunner(
    private val properties: GooglePlacesProperties,
    private val useCase: PlaceBusinessHoursSyncUseCase,
) : ApplicationRunner {
    private val log = KotlinLogging.logger {}

    override fun run(args: ApplicationArguments) {
        if (!properties.backfillOnStartup || properties.apiKey.isBlank()) return
        thread(name = "place-business-hours-backfill", isDaemon = true) {
            try {
                val n = useCase.backfillAll()
                log.info { "장소 영업시간 백필 완료: ${n}건" }
            } catch (e: Exception) {
                log.error(e) { "장소 영업시간 백필 실패(무시): ${e.message}" }
            }
        }
    }
}
