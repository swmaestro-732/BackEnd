package com.example.backend.course.application.port.inbound

/**
 * 인바운드 포트 — 코스 태그 조회(크로스 도메인). 다른 도메인(user 관심 태그 등)이 태그 존재 여부를 확인한다.
 * 도메인 모델을 경계 밖으로 노출하지 않고 id 만 다룬다.
 */
interface TagQueryUseCase {
    /** 주어진 태그 id 중 실제 존재하는 것만 반환한다. */
    fun findExistingTagIds(tagIds: List<Long>): Set<Long>
}
