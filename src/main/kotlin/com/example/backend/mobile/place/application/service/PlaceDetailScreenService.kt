package com.example.backend.mobile.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.mobile.place.application.port.inbound.PlaceDetailScreenUseCase
import com.example.backend.mobile.place.application.port.inbound.dto.PlaceDetailScreenResult
import com.example.backend.mobile.place.application.port.outbound.ScreenPlacePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 장소 상세 화면 조합 서비스 (BFF). 자신의 아웃바운드 포트([ScreenPlacePort])만 호출해 한 화면 응답 재료를 만든다.
 * 타 도메인 인바운드에 직접 의존하지 않아 MSA 분리 시 어댑터만 교체하면 된다.
 *
 * 리뷰·이 근처 코스·저장 여부는 아직 백엔드가 없어 웹 응답에서 빈/false 스텁으로 채운다(MVP 범위).
 */
@Service
@Transactional(readOnly = true)
class PlaceDetailScreenService(
    private val screenPlacePort: ScreenPlacePort,
) : PlaceDetailScreenUseCase {
    override fun getScreen(placeId: Long): PlaceDetailScreenResult {
        val place =
            screenPlacePort.findById(placeId)
                ?: throw BusinessException(PlaceErrorCode.PLACE_NOT_FOUND, "장소를 찾을 수 없습니다: id=$placeId")
        return PlaceDetailScreenResult(
            id = place.id,
            name = place.name,
            category = place.category,
            imageUrl = place.imageUrl,
            latitude = place.latitude,
            longitude = place.longitude,
            address = place.address,
        )
    }
}
