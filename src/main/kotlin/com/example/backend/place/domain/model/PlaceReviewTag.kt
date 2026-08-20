package com.example.backend.place.domain.model

/**
 * 장소 리뷰 태그 — `.ai/taxonomy.md` "장소 리뷰 태그"(공통 14 + 업종별 50 = 64종)의 **코드 정본**.
 *
 * 마스터 테이블(place_review_tags)은 두지 않는다(V4 에서 제거) — 서버가 저장할 것은 태그 코드뿐이고
 * 문구·이모지는 화면 표기라서다. enum 이름(대문자)이 곧 DB 저장 계약이고(place_review_tag_links.tag),
 * API 계약의 태그 코드는 그 소문자 표기([code]) 다 — 요청 `tagCodes`, 응답 태그의 `code`.
 * 값 추가·변경은 taxonomy.md 와 같은 PR 에서 함께 고친다(상수 이름 변경 금지 — 저장된 값이 깨진다).
 *
 * [label]·[icon] 은 taxonomy 정본 표기를 그대로 담아 두어, 태그를 그리는 쪽(리뷰 목록 BFF 등)이
 * 별도 마스터 조회 없이 code → 문구·이모지를 채울 수 있게 한다.
 */
enum class PlaceReviewTag(
    val label: String,
    val icon: String,
) {
    // ── 공통 · 응대·서비스 ──
    FRIENDLY("사장님이 친절해요", "😊"),
    BROWSING("편하게 둘러볼 수 있어요", "👁️"),
    HELPFUL("설명을 잘해줘요", "💬"),
    QUICK("응대가 빨라요", "⚡"),

    // ── 공통 · 공간·환경 ──
    CLEAN("매장이 깨끗해요", "✨"),
    INTERIOR("인테리어가 예뻐요", "🛋️"),
    PHOTO("사진 찍기 좋아요", "📸"),
    COZY("조용하고 아늑해요", "🔮"),
    SPACIOUS("공간이 넓어요", "↔️"),

    // ── 공통 · 접근·편의 ──
    FINDABLE("찾기 쉬워요", "🛣️"),
    PARKING("주차하기 편해요", "🚗"),
    NOWAIT("웨이팅이 적어요", "⏳"),
    SOLO("혼자 가기 좋아요", "👤"),
    REVISIT("또 오고 싶어요", "🔁"),

    // ── 업종별 · 카페·디저트 ──
    COFFEE("커피가 맛있어요", "☕"),
    DESSERT("디저트가 맛있어요", "🍰"),
    SIGNATURE("시그니처 메뉴가 좋아요", "🌟"),
    LINGERING("오래 있기 좋아요", "🕰️"),
    OUTLETS("콘센트가 많아요", "🔌"),
    VIEW("뷰가 좋아요", "🏔️"),

    // ── 업종별 · 음식점 ──
    TASTY("음식이 맛있어요", "😋"),
    FRESH("재료가 신선해요", "🥬"),
    HEARTY("양이 푸짐해요", "🍜"),
    VALUE("가성비가 좋아요", "👛"),
    VARIETY("메뉴가 다양해요", "📋"),
    GROUP("회식하기 좋아요", "👥"),

    // ── 업종별 · 술집·바 ──
    DRINKS("술 종류가 다양해요", "🥂"),
    SNACKS("안주가 맛있어요", "🍖"),
    MOOD("분위기가 좋아요", "🎶"),
    LATE("늦게까지 해요", "🌙"),
    DATE("데이트하기 좋아요", "❤️"),

    // ── 업종별 · 쇼핑·상점 ──
    STYLISH("물건이 감각적이에요", "🪄"),
    EXCLUSIVE("여기서만 파는 게 있어요", "💎"),
    DISCOVERY("구경하는 재미가 있어요", "🔍"),
    GIFTS("선물 고르기 좋아요", "🎁"),

    // ── 업종별 · 문화·전시 ──
    ARTWORK("작품이 인상적이에요", "🖼️"),
    CURATION("큐레이션이 좋아요", "🎧"),
    VIEWING("여유롭게 감상해요", "👁️"),
    HEALING("힐링하기 좋아요", "😌"),

    // ── 업종별 · 체험·클래스 ──
    GUIDANCE("설명이 자세해요", "💬"),
    BEGINNER("초보도 하기 좋아요", "🌱"),
    RESULT("결과물이 만족스러워요", "🏆"),
    MATERIALS("재료가 좋아요", "📦"),
    FOCUS("집중하기 좋아요", "🎯"),

    // ── 업종별 · 자연·아웃도어 ──
    SUNSET("노을이 예뻐요", "🌇"),
    AIR("공기가 맑아요", "💨"),
    REST("조용히 쉬기 좋아요", "🤫"),
    PET("반려동물과 오기 좋아요", "🐾"),
    FAMILY("아이와 나들이하기 좋아요", "👶🏻"),

    // ── 업종별 · 역사·명소 ──
    SERENE("고즈넉해요", "☁️"),
    ARCHITECTURE("건축이 멋져요", "🏢"),
    PEACEFUL("한적해서 여유로워요", "🪶"),

    // ── 업종별 · 여가·엔터테인먼트 ──
    EXCITING("신나고 재밌어요", "😆"),
    TOGETHER("여럿이 즐기기 좋아요", "👥"),
    IMMERSIVE("시간 가는 줄 몰라요", "⏱️"),
    RAINYDAY("날씨 상관없이 즐겨요", "🌧️"),
    DIVERSE("즐길 거리가 다양해요", "🎮"),
    DATEFUN("데이트하기 좋아요", "❤️"),

    // ── 업종별 · 웰니스·힐링 ──
    REFRESHED("몸이 개운해져요", "💆"),
    CALM("마음이 편안해져요", "🧘"),
    EXPERT("관리가 전문적이에요", "✨"),
    HYGIENE("시설이 청결해요", "🛁"),
    TRANQUIL("조용히 쉬기 좋아요", "🌙"),
    METIME("나를 위한 시간이에요", "🌿"),
    ;

    /** API 계약에서 주고받는 태그 코드 — taxonomy 키워드와 1:1(enum 이름의 소문자 표기). */
    val code: String get() = name.lowercase()

    companion object {
        private val BY_CODE = entries.associateBy { it.code }

        /** 태그 코드를 enum 으로 옮긴다. 아는 코드가 아니면 null — 호출부가 400 으로 돌려준다. */
        fun fromCodeOrNull(code: String): PlaceReviewTag? = BY_CODE[code.trim().lowercase()]
    }
}
