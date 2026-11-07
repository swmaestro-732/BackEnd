package com.example.backend.course.application.port.inbound

/**
 * 인바운드 포트 — 전체 코스를 검색 인덱스에 재색인한다(backfill·드리프트 복구).
 * 부팅 러너(bootstrap)가 인덱스가 비었거나 강제 재색인 설정일 때 호출한다.
 */
interface CourseReindexUseCase {
    /** 전체 코스를 재색인하고 색인한 문서 수를 반환한다. */
    fun reindexAll(): Int

    /** 코스 하나를 DB 에서 다시 읽어 색인한다(커밋 후 리스너용). 삭제됐거나 없으면 아무것도 하지 않는다. */
    fun reindex(courseId: Long)
}
