package com.example.backend.mobile.aichat.application.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** [AiChatRegionAliases] — 조사·말버릇 제거와 별칭 → 지역 마스터 검색어 변환을 검증한다. */
class AiChatRegionAliasesTest {
    @Test
    fun `뒤에 붙은 조사와 말버릇을 긴 것부터 반복해서 뗀다`() {
        assertThat(AiChatRegionAliases.normalize("성수에서 가고 싶어요")).isEqualTo("성수")
        assertThat(AiChatRegionAliases.normalize("홍대 쪽으로요!")).isEqualTo("홍대")
        assertThat(AiChatRegionAliases.normalize("  마포구   연남동 근처 ")).isEqualTo("마포구 연남동")
    }

    @Test
    fun `로 는 떼지 않아 을지로·종로 같은 이름이 살아남는다`() {
        assertThat(AiChatRegionAliases.normalize("을지로")).isEqualTo("을지로")
        assertThat(AiChatRegionAliases.normalize("종로에")).isEqualTo("종로")
    }

    @Test
    fun `입력이 조사만큼 짧으면 떼지 않는다`() {
        assertThat(AiChatRegionAliases.normalize("성수")).isEqualTo("성수")
        assertThat(AiChatRegionAliases.normalize("요")).isEqualTo("요")
    }

    @Test
    fun `별칭은 시군구 포함 검색어로 바꾸고 동·역 접미사는 떼고 찾는다`() {
        assertThat(AiChatRegionAliases.searchKeywords("홍대")).containsExactly("마포구 서교동", "마포구 동교동")
        assertThat(AiChatRegionAliases.searchKeywords("망원동")).containsExactly("마포구 망원동")
        assertThat(AiChatRegionAliases.searchKeywords("잠실역")).containsExactly("송파구 잠실동")
        assertThat(AiChatRegionAliases.searchKeywords("강남역")).containsExactly("강남구 역삼동", "서초구 서초동")
    }

    @Test
    fun `별칭이 없으면 입력 그대로 하나다`() {
        assertThat(AiChatRegionAliases.searchKeywords("성동구")).containsExactly("성동구")
    }
}
