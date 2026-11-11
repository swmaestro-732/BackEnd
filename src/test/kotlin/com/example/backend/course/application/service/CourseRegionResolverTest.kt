package com.example.backend.course.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.course.application.port.outbound.CourseRegionFilter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/** [CourseRegionResolver] — prefix 형식·마스터 존재 검증과 광주(29)/전남(46) 가상 시도 매핑을 검증한다. */
class CourseRegionResolverTest {
    private val areaQueryUseCase = mock(AreaQueryUseCase::class.java)
    private val resolver = CourseRegionResolver(areaQueryUseCase)

    private val gwangjuPrefixes = listOf("12210", "12240", "12270", "12300", "12330")

    @Test
    fun `region 이 없으면 필터도 없다`() {
        assertThat(resolver.resolve(null)).isNull()
        verify(areaQueryUseCase, never()).existsAreaWithPrefix(anyString())
    }

    @ParameterizedTest
    @ValueSource(strings = ["11", "4111", "11680"])
    fun `마스터에 있는 시도·구 있는 시·시군구 prefix 는 include 필터가 된다`(region: String) {
        `when`(areaQueryUseCase.existsAreaWithPrefix(region)).thenReturn(true)

        assertThat(resolver.resolve(region)).isEqualTo(CourseRegionFilter(includePrefixes = listOf(region)))
    }

    @Test
    fun `광주(29)는 통합 시도 안의 5개 구 prefix 로 풀리고 마스터는 묻지 않는다`() {
        assertThat(resolver.resolve("29")).isEqualTo(CourseRegionFilter(includePrefixes = gwangjuPrefixes))
        verify(areaQueryUseCase, never()).existsAreaWithPrefix(anyString())
    }

    @Test
    fun `전남(46)은 통합 시도 12 에서 광주 5개 구를 뺀다`() {
        assertThat(resolver.resolve("46"))
            .isEqualTo(CourseRegionFilter(includePrefixes = listOf("12"), excludePrefixes = gwangjuPrefixes))
        verify(areaQueryUseCase, never()).existsAreaWithPrefix(anyString())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "1", "123", "116801", "1168010100", "1a", "서울"])
    fun `2·4·5자리 숫자가 아니면 마스터를 묻지 않고 거절한다`(region: String) {
        assertThatThrownBy { resolver.resolve(region) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("형식")
        verify(areaQueryUseCase, never()).existsAreaWithPrefix(anyString())
    }

    @Test
    fun `형식은 맞아도 마스터에 없는 prefix 는 거절한다`() {
        `when`(areaQueryUseCase.existsAreaWithPrefix("99")).thenReturn(false)

        assertThatThrownBy { resolver.resolve("99") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("알 수 없는 지역")
    }
}
