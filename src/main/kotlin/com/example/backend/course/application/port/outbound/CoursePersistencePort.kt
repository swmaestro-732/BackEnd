package com.example.backend.course.application.port.outbound

import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import java.time.Instant

/** 코스 단건 읽기 모델. deleted_at IS NULL 인 행만 반환한다(상태·공개범위 판정은 서비스가 수행). */
data class CourseDetailRow(
    val id: Long,
    val userId: Long,
    val title: String,
    val coverImageUrl: String?,
    val description: String?,
    /** 코스 카테고리(응답 theme 의 출처). 미선택 draft 는 null. */
    val category: CourseCategory?,
    /** 코스 지역(예: "성수"). 미입력이면 null. */
    val area: String?,
    val tracingsCnt: Int,
    val status: CourseStatus,
    val visibility: CourseVisibility,
    /** 발행 여부(true=게시, false=임시저장). 게시 코스 편집 시 장소 구성 변경 금지 판정에 쓰인다. */
    val isPublished: Boolean,
)

/**
 * 코스 요약 읽기 모델 — 작성자별 코스 목록 등에 쓰는 범용 요약(화면 전용 아님).
 * 발행 여부·정렬 등 목록별 조건은 각 포트 메서드가 정하고, 공개범위(visibility) 판정은 필요한 경우 서비스가 수행한다.
 */
data class CourseSummaryRow(
    val id: Long,
    val userId: Long,
    val title: String,
    val coverImageUrl: String?,
    val category: CourseCategory?,
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
)

/** 코스에 담긴 장소 읽기 모델(장소별 이미지 포함, orderNo 오름차순). */
data class CoursePlaceRow(
    val id: Long,
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    val walkingMinutes: Int?,
    val images: List<CoursePlaceImageRow>,
)

data class CoursePlaceImageRow(
    val imageUrl: String,
    val orderNo: Int,
)

/**
 * 아웃바운드 포트 — 코스 애그리거트 영속(조회·생성). 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 * user 도메인 [com.example.backend.user.application.port.outbound.UserPersistencePort] 와 동일하게
 * 한 포트에서 읽기·쓰기를 함께 다루고, 저장은 도메인 애그리거트([Course])를 받는다.
 */
interface CoursePersistencePort {
    fun findCourseDetail(courseId: Long): CourseDetailRow?

    /** 여러 코스 본문을 한 번에 읽는다(deleted_at IS NULL). 없는 id 는 결과에서 빠진다 — 목록 화면 배치 조회용. */
    fun findCourseDetails(courseIds: List<Long>): List<CourseDetailRow>

    /**
     * 작성자의 발행 코스 요약 목록 — isPublished=true·status=ACTIVE·deleted_at IS NULL, createdAt 내림차순.
     * 공개범위(visibility) 필터는 서비스가 조회자 기준으로 수행한다.
     */
    fun findPublishedByAuthor(authorId: Long): List<CourseSummaryRow>

    /**
     * 작성자의 임시저장 코스 요약 목록 — isPublished=false·status=ACTIVE·deleted_at IS NULL, updatedAt 내림차순.
     */
    fun findDraftsByAuthor(authorId: Long): List<CourseSummaryRow>

    /**
     * 전체 공개(visibility=PUBLIC)·발행·활성·미삭제 코스 요약을 createdAt 내림차순으로 [limit] 개까지 읽는다.
     * 모두 PUBLIC 이라 서비스의 공개범위 필터가 필요 없다(피드 후보용).
     */
    fun findPublishedPublic(limit: Int): List<CourseSummaryRow>

    /** 미삭제(deleted_at IS NULL) 코스가 존재하는지 확인한다(fork 원본 검증 등). */
    fun existsById(courseId: Long): Boolean

    fun findPlaces(courseId: Long): List<CoursePlaceRow>

    /** 여러 코스의 장소들을 courseId 별로(각 코스 안은 orderNo 오름차순) 한 번에 읽는다 — 목록 화면 배치 조회용. */
    fun findPlacesByCourseIds(courseIds: List<Long>): Map<Long, List<CoursePlaceRow>>

    /** 코스 애그리거트(코스·장소·이미지·태그)를 한 트랜잭션으로 저장하고, 저장된 코스(생성 id·DB 생성값 포함)를 반환한다. */
    fun save(course: Course): Course

    /**
     * 이미 영속화된 코스([Course.id] 필수)를 전체 치환한다 — 코스 본문을 갱신하고
     * 장소·이미지·태그 연결을 지운 뒤 요청 값으로 다시 심고, 갱신된 코스(DB 생성값 포함)를 반환한다(한 트랜잭션).
     */
    fun update(course: Course): Course

    /**
     * 코스를 소프트 삭제한다 — deleted_at·updated_at 을 찍고 status 를 DELETED 로 전이한다.
     * 이미 삭제된 행(deleted_at IS NOT NULL)은 건드리지 않으며, 영향받은 행 수를 반환한다(이중 삭제 방어).
     * 자식(장소·이미지·태그)은 지우지 않는다 — 모든 조회가 courses.deleted_at 로 걸러 도달 불가하다.
     */
    fun softDelete(courseId: Long): Int

    /**
     * 저장 수(saves_cnt)를 1 증가시킨다 — 미삭제(deleted_at IS NULL) 행에만 적용하고 영향받은 행 수를 반환한다.
     * 코스 저장(다른 도메인) 시 호출된다. 정합성은 추후 비동기 집계로 옮긴다.
     */
    fun increaseSavesCount(courseId: Long): Int

    /**
     * 저장 수(saves_cnt)를 1 감소시킨다 — 미삭제(deleted_at IS NULL) 행에만 적용하고 영향받은 행 수를 반환한다.
     * 저장 취소(다른 도메인)가 실제로 일어났을 때만 호출된다. 정합성은 추후 비동기 집계로 옮긴다.
     */
    fun decreaseSavesCount(courseId: Long): Int
}
