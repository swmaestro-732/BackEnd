package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.dto.UserLikeTagResult
import com.example.backend.user.application.port.outbound.LikeTagPort
import org.springframework.stereotype.Component

/**
 * 관심 테마(코스 태그) 공통 로직 — 회원가입·프로필 수정·프로필 조회가 함께 쓴다.
 *
 * 태그 마스터는 course 도메인 소유라 user 는 [LikeTagPort] 를 통해서만 접근한다.
 * `user_like_tags` 는 크로스 도메인 참조라 FK 가 없어, 저장 전에 실재하는 태그인지 직접 검증해야 한다.
 */
@Component
class UserLikeTagResolver(
    private val likeTagPort: LikeTagPort,
) {
    /** 중복을 제거하고 태그 마스터에 실재하는지 검증한다(하나라도 없으면 저장 전에 거절). */
    fun validate(tagIds: List<Long>): List<Long> {
        val distinct = tagIds.distinct()
        if (distinct.isEmpty()) return distinct
        val missing = distinct.toSet() - likeTagPort.findExistingTagIds(distinct)
        if (missing.isNotEmpty()) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "존재하지 않는 태그가 포함되어 있습니다: ids=$missing")
        }
        return distinct
    }

    /** 저장된 태그 id 목록을 표시용(id+이름)으로 푼다. 저장 후 삭제된 태그는 숨긴다(관심 지역과 동일 규칙). */
    fun resolve(tagIds: List<Long>): List<UserLikeTagResult> {
        if (tagIds.isEmpty()) return emptyList()
        val names = likeTagPort.findTagNames(tagIds)
        return tagIds.mapNotNull { id -> names[id]?.let { UserLikeTagResult(id = id, name = it) } }
    }
}
