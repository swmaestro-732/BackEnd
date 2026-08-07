package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseSummary

/**
 * 인바운드 포트 — 코스 조회(크로스 도메인·목록). 상세·쓰기는 [CourseUseCase] 가 담당한다.
 *
 * - [existsById] 다른 도메인(user 저장함 등)이 코스 존재 여부를 확인한다. 도메인 모델을 경계 밖으로 노출하지 않는다.
 * - [listByAuthor] 작성자의 발행 코스 요약 목록. viewerId 기준 공개범위(isViewable) 통과분만 내려준다
 *   (작성자 본인 조회면 viewerId==authorId 라 전체 발행분). 비로그인이면 viewerId=null.
 * - [listDraftsByAuthor] 작성자 본인의 임시저장 코스 요약 목록. 공개범위와 무관하게 최근 수정순으로 내려준다.
 * - [listPublic] 전체 공개(PUBLIC) 발행 코스 후보 — 최신순 상한 [limit]. 저장수 랭킹은 BFF 가 조합한다.
 */
interface CourseQueryUseCase {
    /** 미삭제(deleted_at IS NULL) 코스가 존재하는지 확인한다. */
    fun existsById(courseId: Long): Boolean

    fun listByAuthor(
        authorId: Long,
        viewerId: Long?,
    ): List<CourseSummary>

    /** 작성자의 임시저장(isPublished=false)·활성·미삭제 코스를 최근 수정순으로 내려준다. */
    fun listDraftsByAuthor(authorId: Long): List<CourseSummary>

    /**
     * 전체 공개(PUBLIC)·발행·활성 코스를 최신순으로 [limit] 개까지 내려준다.
     * 모두 PUBLIC 이라 공개범위 필터가 필요 없다. 저장수 기준 랭킹은 BFF 화면 조합이 수행한다.
     */
    fun listPublic(limit: Int): List<CourseSummary>
}
