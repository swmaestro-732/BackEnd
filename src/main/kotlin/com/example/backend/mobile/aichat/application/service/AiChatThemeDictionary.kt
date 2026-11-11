package com.example.backend.mobile.aichat.application.service

import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatQuickReply
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.ThemeSlot

/**
 * 테마 칩과 직접 입력 사전. 코스 카테고리 12종(course 도메인 enum 이름 문자열 — BFF 격리)과 분류표 라벨.
 * 직접 입력은 사전에 하나만 걸리면 그 카테고리 필터, 여러 개면 가산, 안 걸리면 문장을 키워드 가산으로 넘긴다.
 */
object AiChatThemeDictionary {
    const val ANY = "ANY"

    private val LABELS: Map<String, String> =
        linkedMapOf(
            "DATE" to "데이트",
            "HEALING" to "힐링·산책",
            "FOOD" to "맛집·먹방",
            "CAFETOUR" to "카페 투어",
            "CULTURE" to "문화·예술",
            "NATURE" to "자연·아웃도어",
            "NIGHTVIEW" to "야경·감성",
            "SHOPPING" to "쇼핑",
            "TRADITION" to "역사·전통",
            "ACTIVITY" to "액티비티",
            "FAMILY" to "가족 나들이",
            "SOLO" to "혼놀",
        )

    private val SYNONYMS: Map<String, List<String>> =
        mapOf(
            "DATE" to listOf("데이트", "연인", "커플", "여자친구", "남자친구", "여친", "남친", "로맨틱", "기념일"),
            "HEALING" to listOf("힐링", "산책", "조용", "쉬", "휴식", "여유", "걷", "느긋", "한적"),
            "FOOD" to listOf("맛집", "먹방", "먹", "음식", "식당", "밥", "맛있", "맛난", "미식", "식도락"),
            "CAFETOUR" to listOf("카페", "커피", "디저트", "베이커리", "빵", "케이크", "브런치"),
            "CULTURE" to listOf("문화", "예술", "전시", "미술", "박물관", "공연", "갤러리", "뮤지엄", "영화"),
            "NATURE" to listOf("자연", "아웃도어", "공원", "숲", "바다", "강변", "한강", "산", "피크닉", "꽃"),
            "NIGHTVIEW" to listOf("야경", "감성", "밤", "노을", "저녁", "루프탑", "불빛"),
            "SHOPPING" to listOf("쇼핑", "상점", "편집샵", "편집숍", "소품", "빈티지", "구경", "백화점"),
            "TRADITION" to listOf("역사", "전통", "한옥", "고궁", "궁", "유적", "문화재", "옛"),
            "ACTIVITY" to listOf("액티비티", "체험", "활동", "클래스", "놀", "스포츠", "레저", "방탈출", "보드"),
            "FAMILY" to listOf("가족", "아이", "부모님", "아기", "나들이", "애들", "엄마", "아빠", "유아"),
            "SOLO" to listOf("혼자", "혼놀", "혼밥", "혼카페", "홀로", "나홀로"),
        )

    private val ANY_WORDS = listOf("상관없", "아무", "없어", "몰라", "딱히", "글쎄", "추천해", "알아서", "다 좋")

    val CATEGORIES: Set<String> = LABELS.keys

    val CHIPS: List<AiChatQuickReply> =
        LABELS.map { (code, label) -> AiChatQuickReply(label, code) } + AiChatQuickReply("상관없어요", ANY)

    fun slotOf(category: String?): ThemeSlot =
        ThemeSlot(category = category, keyword = null, likeThemes = listOfNotNull(category))

    /** 직접 입력 문장 → 테마 슬롯. 비어 있지 않은 문장만 넘긴다. */
    fun classify(text: String): ThemeSlot {
        val sentence = text.trim()
        if (ANY_WORDS.any { sentence.contains(it) }) return slotOf(null)
        val matched = SYNONYMS.filter { (_, words) -> words.any { sentence.contains(it) } }.keys.toList()
        return when (matched.size) {
            0 -> ThemeSlot(category = null, keyword = sentence, likeThemes = emptyList())
            1 -> slotOf(matched.single())
            else -> ThemeSlot(category = null, keyword = null, likeThemes = matched)
        }
    }
}
