package com.example.backend.course.application.port.inbound

/**
 * 인바운드 포트 — 코스 태그 조회(크로스 도메인). 다른 도메인(user 관심 태그 등)이 태그 존재 여부·표시 이름을 확인한다.
 * 도메인 모델을 경계 밖으로 노출하지 않고 id·name 만 다룬다.
 */
interface TagQueryUseCase {
    /** 주어진 태그 id 중 실제 존재하는 것만 반환한다. */
    fun findExistingTagIds(tagIds: List<Long>): Set<Long>

    /** 주어진 태그 id 의 표시 이름을 id → name 으로 반환한다. 없는 id 는 결과에서 빠진다. */
    fun findTagNames(tagIds: List<Long>): Map<Long, String>
}
