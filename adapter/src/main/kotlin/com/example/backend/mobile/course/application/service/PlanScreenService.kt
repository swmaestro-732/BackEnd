package com.example.backend.mobile.course.application.service

import com.example.backend.course.application.port.inbound.PlanQueryUseCase
import com.example.backend.mobile.course.application.port.inbound.PlanScreenUseCase
import com.example.backend.mobile.course.application.port.inbound.dto.PlanDetailScreenResult
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 계획 상세 화면 조합 서비스 (BFF) — 계획 레코드에 장소 요약(이름·카테고리·좌표·대표 이미지)을 붙인다.
 * 소유자 판정은 계획 도메인([PlanQueryUseCase])이 수행하므로 여기서 다시 검사하지 않는다.
 */
@Service
@Transactional(readOnly = true)
class PlanScreenService(
    private val planQueryUseCase: PlanQueryUseCase,
    private val placeQueryUseCase: PlaceQueryUseCase,
) : PlanScreenUseCase {
    override fun getScreen(
        planId: Long,
        userId: Long,
    ): PlanDetailScreenResult {
        val plan = planQueryUseCase.getDetail(planId, userId)
        // 장소는 한 번에 배치 조회한다(항목별 조회 N+1 회피). 같은 장소를 두 번 담았을 수 있어 중복은 제거한다.
        val places = placeQueryUseCase.findPlacesById(plan.places.map { it.placeId }.distinct())
        return PlanDetailScreenResult(plan = plan, places = places)
    }
}
