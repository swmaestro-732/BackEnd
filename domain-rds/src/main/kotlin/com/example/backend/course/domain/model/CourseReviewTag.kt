package com.example.backend.course.domain.model

/**
 * 코스 리뷰 태그 — `.ai/taxonomy.md` "코스 리뷰 태그"(5그룹 24종)의 **코드 정본**.
 *
 * enum 이름(대문자)이 곧 DB 저장 계약이다(course_review_tag_links.tag) — 상수 이름을 바꾸면 저장된 값이 깨지므로 리네임 금지.
 */
enum class CourseReviewTag(
    val label: String,
    val icon: String,
) {
    // ── 코스 구성 ──
    PACKED("구성이 알차요", "📦"),
    COMBO("장소 조합이 좋아요", "🌿"),
    SMOOTH("흐름이 자연스러워요", "🌊"),
    EFFICIENT("동선이 효율적이에요", "🎢"),

    // ── 이동·접근 ──
    WALKABLE("도보로 다니기 좋아요", "👣"),
    TRANSIT("대중교통으로 편해요", "🚌"),
    BYCAR("차 있으면 편해요", "🚗"),
    COMPACT("이동 거리가 짧아요", "📏"),
    STEEP("언덕·계단이 많아요", "🚶‍♂️"),

    // ── 시간·페이스 ──
    HALFDAY("반나절 코스로 좋아요", "⏰"),
    FULLDAY("하루 코스로 좋아요", "🌇"),
    QUICKCOURSE("짧고 알차요", "⏳"),
    RELAXED("여유롭게 즐겨요", "😌"),
    SLOWPACE("천천히 걷기 좋아요", "🐢"),

    // ── 동반자 ──
    DATE("데이트 코스로 좋아요", "❤️"),
    SOLO("혼자 즐기기 좋아요", "👤"),
    FRIENDS("친구랑 가기 좋아요", "👥"),
    FAMILY("가족 나들이로 좋아요", "🏠"),
    KIDS("아이와 함께 좋아요", "👶🏻"),
    PARENTS("부모님과 가기 좋아요", "👒"),

    // ── 비용·만족 ──
    VALUE("가성비 좋은 코스예요", "👛"),
    FREE("무료로 즐길 수 있어요", "🎟️"),
    REVISIT("또 가고 싶어요", "🔁"),
    MEMORABLE("기억에 남아요", "🔖"),
    ;

    /** API 계약에서 주고받는 태그 코드 — taxonomy 키워드와 1:1(enum 이름의 소문자 표기). */
    val code: String get() = name.lowercase()

    companion object {
        fun fromCodeOrNull(code: String): CourseReviewTag? = entries.firstOrNull { it.code == code }
    }
}
