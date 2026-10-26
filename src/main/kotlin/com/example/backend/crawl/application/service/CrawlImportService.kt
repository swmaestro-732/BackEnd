package com.example.backend.crawl.application.service

import com.example.backend.crawl.application.port.inbound.CrawlImportUseCase
import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand
import com.example.backend.crawl.application.port.outbound.CrawlCoursePort
import com.example.backend.crawl.application.port.outbound.CrawlImportPersistencePort
import com.example.backend.crawl.application.port.outbound.CrawlPlacePort
import com.example.backend.crawl.application.port.outbound.CrawlWalkingPort
import com.example.backend.crawl.application.port.outbound.CuratedCourseCommand
import com.example.backend.crawl.application.port.outbound.CuratedCoursePlace
import com.example.backend.crawl.domain.model.CrawlImport
import com.example.backend.crawl.domain.model.CrawlImportStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionOperations

@Service
class CrawlImportService(
    private val persistence: CrawlImportPersistencePort,
    private val places: CrawlPlacePort,
    private val walking: CrawlWalkingPort,
    private val courses: CrawlCoursePort,
    private val transactionOperations: TransactionOperations,
) : CrawlImportUseCase {
    private val log = KotlinLogging.logger {}

    override fun importResult(command: CrawlResultCommand) {
        val existing = transactionOperations.execute { persistence.findByShortcode(command.shortcode) }
        if (existing?.isCompleted == true) return

        val validPlaces = command.places.filter { it.kakaoPlaceId.isNotBlank() }.sortedBy { it.order }
        val rejection =
            when {
                !command.isCourse -> "not course"

                validPlaces.size < 2 -> "fewer than 2 places"

                // 발행 코스는 장소마다 사진이 필요하다(Course 도메인 규칙). 규칙을 우회하지 않고 적재를 거절한다.
                validPlaces.any { place -> place.imageUrls.none { it.isNotBlank() } } -> "missing images"

                else -> null
            }
        if (rejection != null) {
            complete(command) {
                CrawlImport(command.shortcode, command.runId, CrawlImportStatus.REJECTED, reason = rejection)
            }
            return
        }

        // 장소 등록의 외부 지역 조회와 도보 API는 코스 저장 트랜잭션 밖에서 끝낸다.
        val resolved = places.resolve(validPlaces).associateBy { it.kakaoPlaceId }
        val ordered = validPlaces.map { checkNotNull(resolved[it.kakaoPlaceId]) { "장소 등록 실패: ${it.kakaoPlaceId}" } }
        val segments =
            try {
                walking.walkingSegments(ordered.map { it.coordinate })
            } catch (exception: Exception) {
                log.warn(exception) { "크롤링 코스 도보 계산 실패 — 소요 시간 없이 저장: ${command.shortcode}" }
                emptyList()
            }
        val course =
            CuratedCourseCommand(
                title = command.title,
                description = command.description,
                coverImageUrl = validPlaces.asSequence().flatMap { it.imageUrls }.firstOrNull { it.isNotBlank() },
                tags = command.tags,
                places =
                    validPlaces.mapIndexed { index, place ->
                        CuratedCoursePlace(
                            placeId = ordered[index].id,
                            orderNo = place.order,
                            imageUrls = place.imageUrls,
                            walkingMinutes = if (index == validPlaces.lastIndex) null else segments.getOrNull(index),
                        )
                    },
            )
        complete(command) {
            CrawlImport(command.shortcode, command.runId, CrawlImportStatus.IMPORTED, courseId = courses.create(course))
        }
    }

    private fun complete(
        command: CrawlResultCommand,
        result: () -> CrawlImport,
    ) {
        // 단일 DB의 Exposed SpringTransactionManager에서 CourseService의 REQUIRED가 참여한다.
        // 코스와 처리 이력을 함께 커밋해야 재전달에도 중복 코스가 없고 AFTER_COMMIT 이벤트도 함께 지연된다.
        transactionOperations.executeWithoutResult {
            if (!persistence.claim(command.shortcode, command.runId).isCompleted) {
                persistence.save(result())
            }
        }
    }
}
