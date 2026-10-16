package com.example.backend.area.adapter.inbound.web.response

import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.area.domain.model.AreaLevel

/**
 * 시도·시군구·읍면동 공통 조회 응답 DTO. 칩 UI 는 [shortName], 자동완성은 [fullName] 을 쓴다.
 */
data class AreaViewResponse(
    val prefix: String,
    val shortName: String,
    val fullName: String,
    val level: AreaLevel,
) {
    companion object {
        fun from(view: AreaDescriptor): AreaViewResponse =
            AreaViewResponse(
                prefix = view.prefix,
                shortName = view.shortName,
                fullName = view.fullName,
                level = view.level,
            )

        fun from(views: List<AreaDescriptor>): List<AreaViewResponse> = views.map(::from)
    }
}
