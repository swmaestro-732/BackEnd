package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.AuthorCourseCursor
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CourseSummary
import com.example.backend.course.application.port.inbound.dto.CourseSummaryPage

/**
 * 인바운드 포트 — 코스 조회(상세·목록·크로스 도메인). 쓰기는 [CourseUseCase] 가 담당한다.
 *
 * - [getDetail]/[getDetails] 코스 상세 조회. viewerId 는 로그인 사용자(비로그인이면 null) — 조회자 상태·비공개 접근 판정용.
 * - [existsById] 다른 도메인(user 저장함 등)이 코스 존재 여부를 확인한다. 도메인 모델을 경계 밖으로 노출하지 않는다.
 * - [listByAuthor] 작성자의 발행 코스 요약 목록. viewerId 기준 공개범위(isViewable) 통과분만 내려준다
 *   (작성자 본인 조회면 viewerId==authorId 라 전체 발행분). 비로그인이면 viewerId=null.
 * - [listPublic] 전체 공개(PUBLIC) 발행 코스 후보 — 최신순 상한 [limit]. 저장수 랭킹은 BFF 가 조합한다.
 */
interface CourseQueryUseCase {
    /** 코스 상세 단건. status=ACTIVE·미삭제만 반환(그 외 404), PRIVATE 은 소유자만, FOLLOWER 는 소유자·팔로워만. */
    fun getDetail(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailResult

    /** 코스 상세 배치(목록 화면 조합용) — 볼 수 없는 코스는 제외하고 입력 순서를 유지한다. */
    fun getDetails(
        courseIds: List<Long>,
        viewerId: Long?,
    ): List<CourseDetailResult>

    /** 미삭제(deleted_at IS NULL) 코스가 존재하는지 확인한다. */
    fun existsById(courseId: Long): Boolean

    /**
     * 작성자의 조회 가능한 발행 코스를 createdAt DESC, id DESC 순으로 [size]개 내려준다.
     * [cursor]가 있으면 직전 페이지 마지막 정렬 키 이후부터 조회한다.
     */
    fun listByAuthor(
        authorId: Long,
        viewerId: Long?,
        cursor: AuthorCourseCursor?,
        size: Int,
    ): CourseSummaryPage

    /**
     * 전체 공개(PUBLIC)·발행·활성 코스를 최신순으로 [limit] 개까지 내려준다.
     * 모두 PUBLIC 이라 공개범위 필터가 필요 없다. 저장수 기준 랭킹은 BFF 화면 조합이 수행한다.
     */
    fun listPublic(limit: Int): List<CourseSummary>
}
