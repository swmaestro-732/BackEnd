package com.example.backend.mobile.place.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceDetailScreenResponse
import com.example.backend.mobile.place.application.port.inbound.PlaceDetailScreenUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 장소 상세 **화면 조합 API** (BFF).
 * 장소 정보 + 리뷰 요약/미리보기(작성자) + 저장 여부 + "이 근처 코스"(이 장소를 포함한 코스)를 한 번에 내려준다.
 * 개별 도메인 API(`/api/v1/...`)와 구분해 화면 조합 경로로 노출한다(코스 상세 선례와 동일 원칙).
 * 모바일 화면용 경로 `/service/v1/places/{placeId}`(도메인 REST API `/api/v1/...` 와 구분).
 *
 * 조합(장소 조회)은 인바운드 포트([PlaceDetailScreenUseCase])가 담당하고, 컨트롤러는 Request → 포트 호출 → Response 매핑만 한다.
 * 리뷰·이 근처 코스·저장 여부는 아직 백엔드가 없어 빈/false 스텁으로 내려간다(MVP 범위).
 * 존재하지 않는 장소는 404(PLACE_NOT_FOUND).
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 이고 [MockGuard] 가 모킹을 허용하면
 * 조회 없이 고정 목([PlaceDetailScreenResponse.MOCK])을 반환한다.
 * 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@Tag(name = "Place")
@RestController
@RequestMapping("/service/v1")
class PlaceDetailScreenController(
    private val placeDetailScreenUseCase: PlaceDetailScreenUseCase,
    private val mockGuard: MockGuard,
) {
    @GetMapping("/places/{placeId}")
    fun getScreen(
        @PathVariable placeId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<PlaceDetailScreenResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(PlaceDetailScreenResponse.MOCK)
        return ApiResponse.success(PlaceDetailScreenResponse.from(placeDetailScreenUseCase.getScreen(placeId)))
    }
}
