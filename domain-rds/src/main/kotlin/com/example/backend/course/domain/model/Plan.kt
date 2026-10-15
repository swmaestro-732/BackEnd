package com.example.backend.course.domain.model

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.CourseErrorCode
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * 코스 계획 애그리거트 루트 — 코스를 가기 전 소유자만 보는 장소·순서·메모 묶음.
 * 계획(과거) → 따라가기(현재) → 코스 생성(미래) 흐름의 첫 단계라 공개범위·태그·사진·카운터가 없다.
 */
@ConsistentCopyVisibility // copy() 도 private 으로 — 팩토리 우회 차단
data class Plan private constructor(
    val id: Long?,
    val userId: Long,
    val title: String,
    val memo: String?,
    val plannedDate: LocalDate?,
    /** 계획 복제 원본 코스 id. 자유 계획이면 null. 편집으로는 바뀌지 않는다. */
    val sourceCourseId: Long?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val places: List<PlanPlace>,
) {
    companion object {
        /** 계획 한 개에 담을 수 있는 장소 최대 개수 — 코스와 동일. */
        const val MAX_PLACES = 10
        const val MAX_TITLE_LENGTH = 200
        const val MAX_MEMO_LENGTH = 500

        /**
         * 조회·편집·삭제 접근 정책 — 소유자 본인만. 타인 소유는 존재를 드러내지 않도록 404(PLAN_NOT_FOUND).
         * 정책이 바뀌면 이 함수만 고친다 — 서비스는 조회 후 위임만 한다.
         */
        fun ensureOwned(
            ownerId: Long,
            requesterId: Long,
        ) {
            if (ownerId != requesterId) {
                throw BusinessException(CourseErrorCode.PLAN_NOT_FOUND)
            }
        }

        fun create(
            userId: Long,
            title: String,
            memo: String?,
            plannedDate: LocalDate?,
            sourceCourseId: Long?,
            places: List<PlanPlace>,
        ): Plan =
            build(
                id = null,
                userId = userId,
                title = title,
                memo = memo,
                plannedDate = plannedDate,
                sourceCourseId = sourceCourseId,
                places = places,
            )

        /** 편집(전체 치환). 원본 코스([sourceCourseId])는 기존 값을 그대로 받아 유지한다. */
        fun edit(
            id: Long,
            userId: Long,
            title: String,
            memo: String?,
            plannedDate: LocalDate?,
            sourceCourseId: Long?,
            places: List<PlanPlace>,
        ): Plan =
            build(
                id = id,
                userId = userId,
                title = title,
                memo = memo,
                plannedDate = plannedDate,
                sourceCourseId = sourceCourseId,
                places = places,
            )

        /** 생성·편집 공통 — 도메인 불변식을 강제해 애그리거트를 만든다. 장소는 orderNo 오름차순으로 정렬해 담는다. */
        private fun build(
            id: Long?,
            userId: Long,
            title: String,
            memo: String?,
            plannedDate: LocalDate?,
            sourceCourseId: Long?,
            places: List<PlanPlace>,
        ): Plan {
            // 코스(2곳)와 달리 1곳부터 저장할 수 있다 — 계획은 미완성 상태로도 담아 둘 수 있어야 한다.
            if (places.isEmpty()) {
                throw BusinessException(CommonErrorCode.INVALID_INPUT, "계획에는 장소를 1곳 이상 담아야 합니다.")
            }
            if (places.map { it.orderNo }.toSet().size != places.size) {
                throw BusinessException(CommonErrorCode.INVALID_INPUT, "장소 순서(orderNo)가 중복되었습니다.")
            }
            return Plan(
                id = id,
                userId = userId,
                title = title.trim(),
                memo = memo.normalized(),
                plannedDate = plannedDate,
                sourceCourseId = sourceCourseId,
                createdAt = null,
                updatedAt = null,
                places = places.sortedBy { it.orderNo }.map { it.copy(memo = it.memo.normalized()) },
            )
        }

        /**
         * 영속 계층에서 읽어온 상태로 복원한다(insert 직후·조회 시). 이미 저장된 신뢰 값이라 불변식을 재검증하지 않는다.
         * copy() 가 막혀 있어 id·DB 생성값을 채운 [Plan] 을 만드는 유일한 통로다.
         */
        fun reconstitute(
            id: Long,
            userId: Long,
            title: String,
            memo: String?,
            plannedDate: LocalDate?,
            sourceCourseId: Long?,
            createdAt: Instant,
            updatedAt: Instant,
            places: List<PlanPlace>,
        ): Plan =
            Plan(
                id = id,
                userId = userId,
                title = title,
                memo = memo,
                plannedDate = plannedDate,
                sourceCourseId = sourceCourseId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                places = places,
            )

        /** 메모는 트림 후 빈 값이면 null 로 저장한다(리뷰 한마디와 같은 규칙). */
        private fun String?.normalized(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    }
}
