package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.course.application.port.inbound.TagQueryUseCase
import com.example.backend.user.application.port.outbound.LikeTagPort
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [LikeTagPort] 를 course 도메인 인바운드 포트([TagQueryUseCase])에 위임한다(인프로세스).
 * user 애플리케이션이 course 를 직접 알지 않게 하는 경계 지점. MSA 분리 시 이 어댑터만 course 서비스 클라이언트로 교체.
 */
@Component
class LikeTagAdapter(
    private val tagQueryUseCase: TagQueryUseCase,
) : LikeTagPort {
    override fun findExistingTagIds(tagIds: List<Long>): Set<Long> = tagQueryUseCase.findExistingTagIds(tagIds)

    override fun findTagNames(tagIds: List<Long>): Map<Long, String> = tagQueryUseCase.findTagNames(tagIds)
}
