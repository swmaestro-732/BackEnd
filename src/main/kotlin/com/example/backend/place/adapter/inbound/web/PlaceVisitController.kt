package com.example.backend.place.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 장소 방문 처리(RESTful 리소스 경로). **모킹 API**.
 *
 * 장소 리소스의 방문 하위 컬렉션이므로 `POST /api/v1/places/{placeId}/visits` 로 노출한다.
 * 구 경로 `PATCH /api/v1/my/saved-places/{savedPlaceId}`([com.example.backend.user.adapter.inbound.web.SavedPlaceController.visit])
 * 와 동일한 모킹 성공 엔벨로프를 반환한다(신·구 병행 유지). 구 경로는 저장 레코드 id 기준이었으나,
 * 리소스 경로에서는 장소 id 기준이다. 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체한다.
 * PlaceController 의 기존 매핑은 건드리지 않는다.
 */
@RestController
@RequestMapping("/api/v1/places")
class PlaceVisitController {
    @PostMapping("/{placeId}/visits")
    fun visit(
        @PathVariable placeId: Long,
    ): ApiResponse<Nothing?> = ApiResponse.ok("방문이 완료되었습니다.")
}
