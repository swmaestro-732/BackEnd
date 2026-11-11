package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.exception.BusinessException
import com.example.backend.mobile.aichat.application.port.outbound.AiChatPlace
import com.example.backend.mobile.aichat.application.port.outbound.AiChatPlacePort
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 장소 이름 검색을 place 도메인 인바운드 포트에 위임하고 되묻기 칩용 필드로 줄인다.
 * 세션의 지역 prefix 를 함께 넘겨 같은 이름의 다른 지역 장소보다 그 지역 장소를 먼저 찾는다(0건이면 place 가 지역 없이 재검색).
 * 장소 검색은 엔진 전용(503)이라 실패하면 빈 목록으로 흡수한다 — 대화는 "장소 없이 추천"으로 이어갈 수 있다.
 */
@Component
class AiChatPlaceAdapter(
    private val placeQueryUseCase: PlaceQueryUseCase,
    private val areaQueryUseCase: AreaQueryUseCase,
) : AiChatPlacePort {
    private val log = KotlinLogging.logger {}

    override fun searchByName(
        name: String,
        size: Int,
        regionPrefixes: List<String>,
    ): List<AiChatPlace> {
        val page =
            try {
                placeQueryUseCase.searchByName(
                    query = name,
                    cursor = null,
                    size = size,
                    areaCodePrefixes = regionPrefixes,
                )
            } catch (e: BusinessException) {
                log.warn { "ai-chat 장소 검색 실패(빈 결과로 진행): ${e.message}" }
                return emptyList()
            }
        return page.items.map { place ->
            AiChatPlace(
                id = place.id,
                name = place.name,
                categoryLabel = CATEGORY_LABELS[place.category] ?: place.category,
                dongName = place.areaCode?.let { areaQueryUseCase.findAreaByCode(it)?.shortName },
            )
        }
    }

    private companion object {
        /** 장소 카테고리 라벨(위키 taxonomy.md). place 도메인 enum 은 BFF 에서 참조하지 않아 이름 문자열로 둔다. */
        val CATEGORY_LABELS =
            mapOf(
                "CAFE" to "카페·디저트",
                "RESTAURANT" to "음식점",
                "BAR" to "술집·바",
                "SHOPPING" to "쇼핑·상점",
                "CULTURE" to "문화·전시",
                "EXPERIENCE" to "체험·클래스",
                "NATURE" to "자연·아웃도어",
                "LANDMARK" to "역사·명소",
                "ENTERTAINMENT" to "여가·엔터테인먼트",
                "WELLNESS" to "웰니스·힐링",
                "ETC" to "기타",
            )
    }
}
