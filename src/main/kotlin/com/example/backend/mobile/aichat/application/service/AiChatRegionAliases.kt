package com.example.backend.mobile.aichat.application.service

/**
 * 지역 직접 입력 정리. 뒤에 붙은 조사·말버릇을 떼고, 행정구역 이름과 다른 생활권 이름(성수·홍대 등)은
 * 지역 마스터 이름 검색어(시군구 + 읍면동)로 바꾼다. 매칭은 지역 마스터([com.example.backend.area]) 가 한다.
 */
object AiChatRegionAliases {
    /** 긴 것부터 뗀다. "로"는 을지로·종로 같은 이름을 깨뜨리므로 떼지 않는다. */
    private val TRAILERS =
        listOf(
            "가고 싶어요",
            "가고싶어요",
            "가고 싶어",
            "가고싶어",
            "에서요",
            "으로요",
            "갈래요",
            "쪽으로",
            "에서",
            "으로",
            "근처",
            "주변",
            "근방",
            "갈래",
            "가자",
            "이요",
            "에요",
            "요",
            "쪽",
            "에",
        )

    private val ALIASES: Map<String, List<String>> =
        mapOf(
            "성수" to listOf("성동구 성수동"),
            "서울숲" to listOf("성동구 성수동1가"),
            "홍대" to listOf("마포구 서교동", "마포구 동교동"),
            "연남" to listOf("마포구 연남동"),
            "망원" to listOf("마포구 망원동"),
            "합정" to listOf("마포구 합정동"),
            "상수" to listOf("마포구 상수동"),
            "이태원" to listOf("용산구 이태원동"),
            "한남" to listOf("용산구 한남동"),
            "해방촌" to listOf("용산구 용산동2가"),
            "신촌" to listOf("서대문구 창천동", "서대문구 대현동"),
            "건대" to listOf("광진구 화양동"),
            "북촌" to listOf("종로구 가회동", "종로구 계동", "종로구 삼청동"),
            "서촌" to listOf("종로구 통인동", "종로구 효자동", "종로구 체부동"),
            "익선" to listOf("종로구 익선동"),
            "을지로" to listOf("중구 을지로"),
            "강남" to listOf("강남구"),
            "강남역" to listOf("강남구 역삼동", "서초구 서초동"),
            "압구정" to listOf("강남구 압구정동"),
            "가로수길" to listOf("강남구 신사동"),
            "잠실" to listOf("송파구 잠실동"),
            "석촌" to listOf("송파구 석촌동"),
            "여의도" to listOf("영등포구 여의도동"),
            "문래" to listOf("영등포구 문래동"),
            "성신여대" to listOf("성북구 동선동"),
            "경리단길" to listOf("용산구 이태원동"),
            "송리단길" to listOf("송파구 송파동"),
            "샤로수길" to listOf("관악구 봉천동"),
        )

    fun normalize(text: String): String {
        var result = text.trim().trimEnd('.', '!', '?', '~', ',')
        var changed = true
        while (changed) {
            changed = false
            for (trailer in TRAILERS) {
                if (result.length > trailer.length + 1 && result.endsWith(trailer)) {
                    result = result.removeSuffix(trailer).trim()
                    changed = true
                }
            }
        }
        return result.replace(Regex("\\s+"), " ")
    }

    /** 정리된 입력 → 지역 마스터 검색어 목록. 별칭이 없으면 입력 그대로 하나. */
    fun searchKeywords(normalized: String): List<String> =
        ALIASES[normalized]
            ?: ALIASES[normalized.removeSuffix("동")]
            ?: ALIASES[normalized.removeSuffix("역")]
            ?: listOf(normalized)
}
