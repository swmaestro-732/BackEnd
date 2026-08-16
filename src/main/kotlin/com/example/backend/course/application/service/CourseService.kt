package com.example.backend.course.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.application.event.CourseAuthorWithdrawnEvent
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.application.port.outbound.AuthorCourseCountPort
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceImageRow
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CoursePlace
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 쓰기(커맨드) 유스케이스 — 생성·편집·삭제. 조회는 [CourseQueryService] 가 담당한다(커맨드/쿼리 분리).
 *
 * 생성/편집: 발행·임시저장 공통. 불변식 검증과 카테고리 도출은 [Course] 애그리거트가 수행하고,
 * 서비스는 카테고리 도출에 필요한 place 카테고리(아웃바운드 [PlaceLookupPort], ACL)만 조회해 넘긴다.
 * 코스 발행/공개범위변경/삭제로 작성자의 공개범위별 코스 개수가 바뀌면 [AuthorCourseCountPort] 로 반영한다.
 */
@Service
@Transactional
class CourseService(
    private val coursePersistencePort: CoursePersistencePort,
    private val placeLookupPort: PlaceLookupPort,
    private val areaQueryUseCase: AreaQueryUseCase,
    private val authorCourseCountPort: AuthorCourseCountPort,
    private val eventPublisher: ApplicationEventPublisher,
) : CourseUseCase {
    override fun create(command: CreateCourseCommand): Course {
        // fork 원본 코스가 실제로 존재하는지 검증(없으면 404).
        command.forkedFromId?.let { forkedFromId ->
            if (!coursePersistencePort.existsById(forkedFromId)) {
                throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "원본 코스를 찾을 수 없습니다: id=$forkedFromId")
            }
        }

        // 참조 place 존재 검증(발행·임시저장 공통 — place_id 는 FK 가 없어 여기서만 걸러진다).
        val foundPlaces = requirePlacesExist(command.places.map { it.placeId })
        val places =
            command.places.map {
                CoursePlace(
                    placeId = it.placeId,
                    orderNo = it.orderNo,
                    caption = it.caption,
                    imageUrls = it.imageUrls,
                    walkingMinutes = it.walkingMinutes,
                )
            }

        // 파생 값(카테고리·지역코드·지역 이름) 도출 — 규칙은 도메인 순수 함수가, 조회(place 요약·지역 이름)는 서비스가 담당한다.
        // 발행 코스만 도출하며(deriveXxx 가 임시저장이면 null), 검증차 조회한 place 요약을 그대로 재사용한다.
        val category =
            Course.deriveCategory(command.isPublished, places, foundPlaces.associate { it.id to it.category })
        val areaCode =
            Course.deriveAreaCode(command.isPublished, places, foundPlaces.associate { it.id to it.areaCode })

        val course =
            Course.create(
                userId = command.userId,
                title = command.title,
                description = command.description,
                coverImageUrl = command.coverImageUrl,
                visibility = command.visibility,
                isPublished = command.isPublished,
                forkedFromId = command.forkedFromId,
                tags = command.tags,
                places = places,
                category = category,
                areaCode = areaCode,
                area = resolveAreaName(areaCode),
            )
        val saved = coursePersistencePort.save(course)
        // 발행 코스만 개수에 잡힌다(임시저장은 제외). 발행이면 해당 공개범위 버킷 +1.
        adjustAuthorCourseCount(
            userId = command.userId,
            removed = null,
            added = if (command.isPublished) command.visibility else null,
        )
        eventPublisher.publishEvent(CourseSavedEvent(saved)) // 커밋 후 검색 색인(이벤트 — AFTER_COMMIT 리스너)
        return saved
    }

    override fun edit(command: EditCourseCommand): Course {
        // 존재·소유권 검증 — 없거나(삭제 포함)·비활성·타인 소유면 존재를 드러내지 않도록 404(COURSE_NOT_FOUND).
        val existing =
            coursePersistencePort.findCourseDetail(command.courseId)
                ?: throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=${command.courseId}")
        if (existing.status != CourseStatus.ACTIVE || existing.userId != command.userId) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=${command.courseId}")
        }

        val places =
            command.places.map {
                CoursePlace(
                    placeId = it.placeId,
                    orderNo = it.orderNo,
                    caption = it.caption,
                    imageUrls = it.imageUrls,
                    walkingMinutes = it.walkingMinutes,
                )
            }

        // 이미 게시된 코스는 장소 구성(place_id·순서·사진)을 바꿀 수 없고 캡션만 수정할 수 있다.
        // (임시저장→발행 전환은 existing.isPublished 가 false 라 이 제약에서 자유롭다.)
        if (existing.isPublished) {
            val storedPlaces = coursePersistencePort.findPlaces(command.courseId)
            if (placesStructureChanged(storedPlaces, places)) {
                throw BusinessException(
                    ErrorCode.PUBLISHED_COURSE_PLACES_IMMUTABLE,
                    "게시된 코스는 장소를 추가·삭제·교체하거나 순서·사진을 바꿀 수 없습니다: id=${command.courseId}",
                )
            }
        }

        // 참조 place 존재 검증(발행·임시저장 공통 — place_id 는 FK 가 없어 여기서만 걸러진다).
        // 발행 코스의 장소 구성 불변(4003) 검증 뒤에 둬 기존 오류 우선순위를 유지한다.
        val foundPlaces = requirePlacesExist(places.map { it.placeId })

        // 카테고리·지역코드 유지/재도출 판정이 공유한다 — placesChanged 는 DB 조회라 발행 편집에서 한 번만 계산한다.
        val placesUnchanged = command.isPublished && !placesChanged(command.courseId, places)
        val areaCode =
            resolveEditedDerivedValue(command.isPublished, existing.areaCode, placesUnchanged) {
                Course.deriveAreaCode(command.isPublished, places, foundPlaces.associate { it.id to it.areaCode })
            }

        val course =
            Course.edit(
                id = command.courseId,
                userId = command.userId,
                title = command.title,
                description = command.description,
                coverImageUrl = command.coverImageUrl,
                visibility = command.visibility,
                isPublished = command.isPublished,
                tags = command.tags,
                places = places,
                category =
                    resolveEditedDerivedValue(command.isPublished, existing.category, placesUnchanged) {
                        Course.deriveCategory(
                            command.isPublished,
                            places,
                            foundPlaces.associate { it.id to it.category },
                        )
                    },
                areaCode = areaCode,
                // 지역 이름은 코드에서 결정적으로 풀리므로 유지/재도출 판정 없이 항상 코드로 재해석한다.
                area = resolveAreaName(areaCode),
            )
        val updated = coursePersistencePort.update(course)
        // 편집은 ACTIVE 코스만 통과한다(위 가드). 발행 상태·공개범위 전이만큼 작성자 카운터를 조정한다
        // (초안→발행 +1, 발행→초안 −1, 공개범위 변경 시 old −1·new +1, 변화 없으면 0).
        adjustAuthorCourseCount(
            userId = command.userId,
            removed = if (existing.isPublished) existing.visibility else null,
            added = if (command.isPublished) command.visibility else null,
        )
        eventPublisher.publishEvent(CourseSavedEvent(updated)) // 커밋 후 검색 색인(이벤트 — AFTER_COMMIT 리스너)
        return updated
    }

    /**
     * 코스 소프트 삭제. 존재·소유권을 검증한 뒤 deleted_at 스탬프만 찍는다(전체 치환·애그리거트 재구성 없음).
     * 자식(장소·이미지·태그)은 그대로 두며, 모든 조회가 courses.deleted_at 로 걸러 도달 불가하다.
     */
    override fun delete(
        userId: Long,
        courseId: Long,
    ) {
        // 존재·소유권 검증 — 없거나(삭제 포함)·비활성·타인 소유면 존재를 드러내지 않도록 404(COURSE_NOT_FOUND).
        val existing =
            coursePersistencePort.findCourseDetail(courseId)
                ?: throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")
        if (existing.status != CourseStatus.ACTIVE || existing.userId != userId) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")
        }

        coursePersistencePort.softDelete(courseId)
        // 발행 코스였다면 삭제로 해당 공개범위 버킷 −1(임시저장은 애초에 안 잡혀 있었다).
        adjustAuthorCourseCount(
            userId = userId,
            removed = if (existing.isPublished) existing.visibility else null,
            added = null,
        )
        eventPublisher.publishEvent(CourseDeletedEvent(courseId)) // 커밋 후 검색 색인(이벤트 — AFTER_COMMIT 리스너)
    }

    override fun deleteAllByAuthor(authorId: Long) {
        // 회원 탈퇴 정리 — 작성자의 살아있는 코스를 전부 소프트 삭제한다.
        // 작성자 공개범위별 카운터는 user 도메인이 탈퇴 시 users 행과 함께 0으로 리셋하므로 여기선 코스 행만 정리한다.
        coursePersistencePort.softDeleteAllByAuthor(authorId)
        eventPublisher.publishEvent(CourseAuthorWithdrawnEvent(authorId)) // 커밋 후 검색 색인(이벤트 — AFTER_COMMIT 리스너)
    }

    /**
     * 편집 코스의 파생 값(카테고리·지역코드)을 해석한다 — 두 값이 같은 유지/재도출 규칙을 공유한다.
     * - 임시저장이면 null(생성과 동일).
     * - 발행이면서 **기존 값이 있고 장소 구성이 그대로면**([placesUnchanged]) 재도출 없이 기존 값을 유지한다 —
     *   장소를 바꾸지 않은 편집(제목·설명 등)에서 외부 place 데이터 드리프트로 값이 바뀌는 것을 막는다.
     * - 그 외(초안→발행, 장소 변경, 값 미지정 코스)에만 [derive] 로 place 데이터에서 도출한다(생성과 동일 규칙).
     */
    private fun <T : Any> resolveEditedDerivedValue(
        isPublished: Boolean,
        existingValue: T?,
        placesUnchanged: Boolean,
        derive: () -> T?,
    ): T? {
        if (!isPublished) return null
        if (existingValue != null && placesUnchanged) return existingValue
        return derive()
    }

    /** 지역코드(법정동코드)를 표시 이름으로 푼다 — 동 레벨은 동 이름("성수동1가"), 시군구 레벨은 시군구 이름("강남구"). */
    private fun resolveAreaName(areaCode: String?): String? =
        areaCode?.let { areaQueryUseCase.findAreaByCode(it)?.shortName }

    /**
     * 발행 코스가 참조하는 place_id 가 모두 실제로 존재하는지 검증하고, 조회한 장소 요약을 돌려준다.
     */
    private fun requirePlacesExist(placeIds: List<Long>): List<PlaceRef> {
        val requestedIds = placeIds.distinct()
        val found = placeLookupPort.findPlacesByIds(requestedIds)
        val missing = requestedIds.filterNot { id -> found.any { it.id == id } }
        if (missing.isNotEmpty()) {
            throw BusinessException(ErrorCode.PLACE_NOT_FOUND, "존재하지 않는 장소가 포함되어 있습니다: ids=$missing")
        }
        return found
    }

    /**
     * 게시된 코스 편집에서 **캡션을 제외한** 장소 구성이 바뀌었는지 판정한다.
     * place_id·순서(orderNo)·사진(imageUrls)이 저장본과 완전히 같아야 false(변경 없음)다.
     * 저장본 이미지는 orderNo 오름차순으로 조회되므로 imageUrl 나열이 요청 imageUrls 순서와 그대로 대응한다.
     */
    private fun placesStructureChanged(
        stored: List<CoursePlaceRow>,
        newPlaces: List<CoursePlace>,
    ): Boolean {
        if (stored.size != newPlaces.size) return true
        val storedSignature =
            stored
                .sortedBy { it.orderNo }
                .map { Triple(it.placeId, it.orderNo, it.images.map(CoursePlaceImageRow::imageUrl)) }
        val newSignature =
            newPlaces
                .sortedBy { it.orderNo }
                .map { Triple(it.placeId, it.orderNo, it.imageUrls) }
        return storedSignature != newSignature
    }

    /** 저장된 코스의 장소 구성이 요청과 다른지 판정한다 — 카테고리는 orderNo 순 placeId 나열에만 의존한다. */
    private fun placesChanged(
        courseId: Long,
        newPlaces: List<CoursePlace>,
    ): Boolean {
        val existingPlaceIds =
            coursePersistencePort.findPlaces(courseId).sortedBy { it.orderNo }.map { it.placeId }
        val newPlaceIds = newPlaces.sortedBy { it.orderNo }.map { it.placeId }
        return existingPlaceIds != newPlaceIds
    }

    /**
     * 코스 상태 변화에 따른 작성자의 공개범위별 코스 개수 델타를 반영한다.
     * [removed]/[added] 는 "카운트되는 상태(발행·활성·미삭제)"의 공개범위이고, 그 상태가 아니면 null.
     * 크로스 도메인 경계라 공개범위 enum 대신 버킷별 원시 int 델타로 넘긴다([AuthorCourseCountPort]).
     */
    private fun adjustAuthorCourseCount(
        userId: Long,
        removed: CourseVisibility?,
        added: CourseVisibility?,
    ) {
        fun delta(v: CourseVisibility) = (if (added == v) 1 else 0) - (if (removed == v) 1 else 0)
        authorCourseCountPort.applyDelta(
            authorId = userId,
            publicDelta = delta(CourseVisibility.PUBLIC),
            followerDelta = delta(CourseVisibility.FOLLOWER),
            privateDelta = delta(CourseVisibility.PRIVATE),
        )
    }
}
