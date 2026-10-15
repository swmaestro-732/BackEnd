package com.example.backend.bootstrap.search

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

/**
 * 인덱스 매핑 리소스(opensearch 의 course.json, place.json)가 복합어 정확도용 korean 분석기 설정을 담고 있는지
 * Docker 없이 검증한다(SCRUM-552). 통합테스트(opensearchIt)는 실제 nori 컨테이너로 동작을 보지만,
 * 이 단위 테스트는 설정 회귀(nori 로 되돌림, decompound_mode 누락, JSON 깨짐)를 빠르게 잡는다.
 */
class OpenSearchMappingResourceTest {
    private val mapper = ObjectMapper()

    private fun load(name: String): JsonNode =
        ClassPathResource("opensearch/$name").inputStream.use { mapper.readTree(it) }

    private fun textFieldAnalyzers(root: JsonNode): Map<String, String> {
        val props = root.path("mappings").path("properties")
        return props
            .fieldNames()
            .asSequence()
            .filter { props.path(it).path("type").asText() == "text" }
            .associateWith { props.path(it).path("analyzer").asText() }
    }

    @Test
    fun `course 매핑은 mixed decompound 의 korean 분석기를 정의한다`() {
        assertKoreanAnalyzerDefined(load("course.json"))
    }

    @Test
    fun `place 매핑은 mixed decompound 의 korean 분석기를 정의한다`() {
        assertKoreanAnalyzerDefined(load("place.json"))
    }

    private fun assertKoreanAnalyzerDefined(root: JsonNode) {
        val analysis = root.path("settings").path("analysis")
        val tokenizer = analysis.path("tokenizer").path("korean_nori")
        assertThat(tokenizer.path("type").asText()).isEqualTo("nori_tokenizer")
        assertThat(tokenizer.path("decompound_mode").asText()).isEqualTo("mixed")
        assertThat(tokenizer.path("user_dictionary_rules").size()).isGreaterThan(0)

        val analyzer = analysis.path("analyzer").path("korean")
        assertThat(analyzer.path("type").asText()).isEqualTo("custom")
        assertThat(analyzer.path("tokenizer").asText()).isEqualTo("korean_nori")
        assertThat(analyzer.path("filter").map { it.asText() }).contains("nori_part_of_speech", "lowercase")
    }

    @Test
    fun `course 의 모든 text 필드는 korean 분석기를 쓴다(nori 잔존 금지)`() {
        val analyzers = textFieldAnalyzers(load("course.json"))
        assertThat(analyzers.keys).contains("title", "description")
        assertThat(analyzers.values).isNotEmpty.allMatch { it == "korean" }
        assertThat(analyzers.values).doesNotContain("nori")
    }

    @Test
    fun `place 의 모든 text 필드는 korean 분석기를 쓴다(nori 잔존 금지)`() {
        val analyzers = textFieldAnalyzers(load("place.json"))
        assertThat(analyzers.keys).contains("name", "description", "address")
        assertThat(analyzers.values).isNotEmpty.allMatch { it == "korean" }
        assertThat(analyzers.values).doesNotContain("nori")
    }

    @Test
    fun `course 와 place 는 동일한 분석기 설정을 공유한다`() {
        val courseSettings = load("course.json").path("settings")
        val placeSettings = load("place.json").path("settings")
        assertThat(courseSettings).isEqualTo(placeSettings)
    }
}
