package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.course.application.port.inbound.CourseCategoryQueryUseCase
import com.example.backend.user.application.port.outbound.LikeThemePort
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [LikeThemePort] 를 course 인바운드 포트([CourseCategoryQueryUseCase])에 위임한다(인프로세스).
 * user 애플리케이션이 course 를 직접 알지 않게 하는 경계 지점 — enum 을 복제하지 않고 이름 문자열만 받는다.
 */
@Component
class LikeThemeAdapter(
    private val courseCategoryQueryUseCase: CourseCategoryQueryUseCase,
) : LikeThemePort {
    override fun listThemeNames(): List<String> = courseCategoryQueryUseCase.listCategoryNames()
}
