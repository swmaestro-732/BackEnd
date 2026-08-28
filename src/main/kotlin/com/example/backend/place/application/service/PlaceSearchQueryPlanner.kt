package com.example.backend.place.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.place.domain.model.PlaceCategory
import org.springframework.stereotype.Component

/**
 * 검색어를 공백 토큰으로 나눠 검색 실행 계획으로 변환한다 — "성수 카페" → areaCode prefix(성수동*) + category(CAFE).
 *
 * 토큰별 분류 우선순위: (a) 카테고리 동의어 사전 → 카테고리 필터, (b) 지역 디렉터리(법정동 이름 contains 매치,
 * [AreaQueryUseCase.searchAreas]) → areaCode prefix 필터, (c) 나머지 → 텍스트 토큰(multi_match).
 * 사전이 지역보다 우선한다 — 같은 토큰이 둘 다 맞으면 카테고리로만 소비한다(예: "문화" → CULTURE, 문화동 아님).
 * 오분류(지역어처럼 보이는 상호 등)는 호출부의 0건 폴백(필터 없는 전체 텍스트 재검색)이 받쳐준다.
 */
@Component
class PlaceSearchQueryPlanner(
    private val areaQueryUseCase: AreaQueryUseCase,
) {
    fun plan(query: String): PlaceSearchPlan {
        val tokens = query.trim().split(WHITESPACE).filter { it.isNotBlank() }

        val textTokens = mutableListOf<String>()
        val categories = mutableListOf<PlaceCategory>()
        val areaPrefixes = mutableListOf<String>()

        tokens.forEach { token ->
            val category = resolveCategory(token)
            if (category != null) {
                categories += category
                return@forEach
            }
            val prefixes = resolveAreaPrefixes(token)
            if (prefixes.isNotEmpty()) {
                areaPrefixes += prefixes
                return@forEach
            }
            textTokens += token
        }

        return PlaceSearchPlan(
            textTokens = textTokens,
            categories = categories.distinct(),
            areaCodePrefixes = dedupeSubsumed(areaPrefixes),
        )
    }

    private fun resolveCategory(token: String): PlaceCategory? =
        CATEGORY_SYNONYMS[token] ?: runCatching { PlaceCategory.valueOf(token.uppercase()) }.getOrNull()

    private fun resolveAreaPrefixes(token: String): List<String> {
        if (token.length < MIN_AREA_TOKEN_LENGTH) return emptyList()
        return areaQueryUseCase.searchAreas(token).map { it.prefix }
    }

    private fun dedupeSubsumed(prefixes: List<String>): List<String> {
        val kept = mutableListOf<String>()
        prefixes.distinct().sortedBy { it.length }.forEach { prefix ->
            if (kept.none { prefix.startsWith(it) }) kept += prefix
        }
        return kept
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
        const val MIN_AREA_TOKEN_LENGTH = 2

        val CATEGORY_SYNONYMS: Map<String, PlaceCategory> =
            buildMap {
                listOf("카페", "까페", "커피", "cafe", "디저트", "베이커리", "빵집").forEach { put(it, PlaceCategory.CAFE) }
                listOf("맛집", "식당", "음식점", "레스토랑", "밥집", "브런치").forEach { put(it, PlaceCategory.RESTAURANT) }
                listOf("술집", "바", "주점", "포차", "펍", "와인바", "칵테일바").forEach { put(it, PlaceCategory.BAR) }
                listOf("쇼핑", "상점", "편집샵", "소품샵", "마켓").forEach { put(it, PlaceCategory.SHOPPING) }
                listOf("문화", "전시", "전시회", "미술관", "박물관", "공연", "갤러리").forEach { put(it, PlaceCategory.CULTURE) }
                listOf("체험", "공방", "클래스", "원데이클래스").forEach { put(it, PlaceCategory.EXPERIENCE) }
                listOf("자연", "공원", "산책", "산책로", "숲").forEach { put(it, PlaceCategory.NATURE) }
                listOf("랜드마크", "명소").forEach { put(it, PlaceCategory.LANDMARK) }
                listOf("놀거리", "오락", "게임", "노래방", "볼링장", "만화카페").forEach { put(it, PlaceCategory.ENTERTAINMENT) }
                listOf("웰니스", "스파", "사우나", "마사지", "찜질방").forEach { put(it, PlaceCategory.WELLNESS) }
            }
    }
}

/** 토큰 분류 결과. 필터가 있는데 결과가 0건이면 호출부가 필터 없는 전체 텍스트 재검색을 1회 한다. */
data class PlaceSearchPlan(
    val textTokens: List<String>,
    val categories: List<PlaceCategory>,
    val areaCodePrefixes: List<String>,
)
