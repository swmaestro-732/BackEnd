package com.example.backend.crawl.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.crawl.application.port.inbound.CrawlImportUseCase
import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand
import com.example.backend.crawl.application.port.inbound.dto.CrawledPlaceCommand
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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.net.URI
import java.net.URISyntaxException

/** 결정적 입력 오류는 REJECTED로 확정해 ack하고, INVALID_INPUT 이력은 실패한 트랜잭션 밖에서 기록한다. */
@Service
class CrawlImportService(
    private val persistence: CrawlImportPersistencePort,
    private val places: CrawlPlacePort,
    private val walking: CrawlWalkingPort,
    private val courses: CrawlCoursePort,
    transactionManager: PlatformTransactionManager,
) : CrawlImportUseCase {
    private val log = KotlinLogging.logger {}
    private val transactionOperations =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    override fun importResult(command: CrawlResultCommand) {
        val existing = transactionOperations.execute { persistence.findByShortcode(command.shortcode) }
        if (existing?.isCompleted == true) return

        val validPlaces =
            command.places
                .filter { it.kakaoPlaceId.isNotBlank() }
                .sortedBy { it.order }
                .map { it.copy(imageUrls = it.imageUrls.filter(::isHttpsImage)) }
        val rejection =
            when {
                !command.isCourse -> "not course"
                validPlaces.size < 2 -> "fewer than 2 places"
                command.title.isBlank() -> "blank title"
                command.title.length > 200 -> "title exceeds 200 characters"
                validPlaces.map { it.order }.distinct().size != validPlaces.size -> "duplicate order"
                command.tags.any { it.length > 50 } -> "tag exceeds 50 characters"
                else -> validPlaces.firstNotNullOfOrNull(::invalidPlaceReason)
            }
        if (rejection != null) {
            reject(command, rejection)
            return
        }

        try {
            importCourse(command, validPlaces)
        } catch (exception: BusinessException) {
            if (exception.errorCode != CommonErrorCode.INVALID_INPUT) throw exception
            // 등록/코스 트랜잭션이 롤백된 뒤 별도 트랜잭션에서 거절을 확정해야 재전달을 종료할 수 있다.
            reject(command, "invalid: ${exception.message}")
        }
    }

    private fun importCourse(
        command: CrawlResultCommand,
        validPlaces: List<CrawledPlaceCommand>,
    ) {
        // 장소 등록의 외부 지역 조회와 도보 API는 코스 저장 트랜잭션 밖에서 끝낸다.
        val resolved = places.resolve(validPlaces).associateBy { it.kakaoPlaceId }
        val ordered = validPlaces.map { checkNotNull(resolved[it.kakaoPlaceId]) { "장소 등록 실패: ${it.kakaoPlaceId}" } }
        val images =
            validPlaces.mapIndexed { index, place ->
                place.imageUrls.ifEmpty { listOfNotNull(ordered[index].imageUrl).filter(::isHttpsImage) }
            }
        if (images.any { it.isEmpty() }) {
            reject(command, "missing images")
            return
        }
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
                coverImageUrl = images.first().first(),
                tags = command.tags,
                places =
                    validPlaces.mapIndexed { index, _ ->
                        CuratedCoursePlace(
                            placeId = ordered[index].id,
                            orderNo = index + 1,
                            imageUrls = images[index],
                            walkingMinutes = if (index == validPlaces.lastIndex) null else segments.getOrNull(index),
                        )
                    },
            )
        complete(command) {
            CrawlImport(command.shortcode, command.runId, CrawlImportStatus.IMPORTED, courseId = courses.create(course))
        }
    }

    private fun invalidPlaceReason(place: CrawledPlaceCommand): String? =
        when {
            !place.lat.isFinite() || place.lat !in -90.0..90.0 -> "invalid latitude: ${place.kakaoPlaceId}"
            !place.lng.isFinite() || place.lng !in -180.0..180.0 -> "invalid longitude: ${place.kakaoPlaceId}"
            place.name.isBlank() -> "blank place name: ${place.kakaoPlaceId}"
            place.address.isBlank() -> "blank place address: ${place.kakaoPlaceId}"
            else -> null
        }

    private fun isHttpsImage(url: String): Boolean =
        try {
            val uri = URI(url)
            uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
        } catch (_: URISyntaxException) {
            false
        }

    private fun reject(
        command: CrawlResultCommand,
        reason: String,
    ) = complete(command) {
        CrawlImport(command.shortcode, command.runId, CrawlImportStatus.REJECTED, reason = reason)
    }

    private fun complete(
        command: CrawlResultCommand,
        result: () -> CrawlImport,
    ) {
        // 단일 DB의 Exposed SpringTransactionManager에서 CourseService의 REQUIRED가 참여한다.
        // 코스와 처리 이력을 함께 커밋해야 재전달에도 중복 코스가 없고 AFTER_COMMIT 이벤트도 함께 지연된다.
        transactionOperations.executeWithoutResult {
            if (persistence.claim(command.shortcode)?.isCompleted != true) {
                persistence.save(result())
            }
        }
    }
}
