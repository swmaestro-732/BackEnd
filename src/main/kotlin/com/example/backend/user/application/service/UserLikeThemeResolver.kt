package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.outbound.LikeThemePort
import org.springframework.stereotype.Component

/**
 * 관심 테마 공통 로직 — 회원가입·프로필 수정이 함께 쓴다.
 *
 * 관심 테마의 정본은 course 도메인의 코스 카테고리(`.ai/taxonomy.md` 12종)라 [LikeThemePort] 로만 접근한다.
 * enum 을 user 쪽에 복제하지 않는 이유: 값이 어긋나면 `courses.category` 와 매칭이 조용히 깨진다(추천 정합성).
 * 저장은 이름 문자열이고 FK 가 없으므로, 저장 전에 유효한 이름인지 직접 검증해야 한다.
 *
 * 조회는 저장된 이름을 그대로 내보내므로 별도 해석 단계가 없다 — 한글 라벨 매핑은 클라이언트 담당
 * (코스 상세의 `theme` 와 동일 규칙).
 */
@Component
class UserLikeThemeResolver(
    private val likeThemePort: LikeThemePort,
) {
    /** 중복을 제거하고 유효한 코스 카테고리 이름인지 검증한다(하나라도 아니면 저장 전에 거절). */
    fun validate(themes: List<String>): List<String> {
        val distinct = themes.distinct()
        if (distinct.isEmpty()) return distinct
        val unknown = distinct - likeThemePort.listThemeNames().toSet()
        if (unknown.isNotEmpty()) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "존재하지 않는 관심 테마가 포함되어 있습니다: $unknown")
        }
        return distinct
    }
}
