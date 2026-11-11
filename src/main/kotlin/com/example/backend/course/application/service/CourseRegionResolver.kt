package com.example.backend.course.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.course.application.port.outbound.CourseRegionFilter
import org.springframework.stereotype.Component

/**
 * 추천 전체보기 `region` 값 → [CourseRegionFilter]. 값은 법정동코드 prefix(시도 2자리·구 있는 시 4자리·시군구 5자리)다.
 *
 * area 마스터는 광주·전남을 통합 시도 코드 `12` 로 갖고 있어 둘을 나누려면 가상 시도 값을 둔다:
 * `29`(옛 광주 코드)는 광주 5개 구 prefix 합집합, `46`(옛 전남 코드)은 `12` 에서 그 5개 구를 뺀 것.
 * 형식이 틀리거나 마스터에 없는 prefix 는 [IllegalArgumentException](400).
 */
@Component
class CourseRegionResolver(
    private val areaQueryUseCase: AreaQueryUseCase,
) {
    fun resolve(region: String?): CourseRegionFilter? =
        when (region) {
            null -> {
                null
            }

            GWANGJU_VIRTUAL_SIDO -> {
                CourseRegionFilter(includePrefixes = GWANGJU_SIGUNGU_PREFIXES)
            }

            JEONNAM_VIRTUAL_SIDO -> {
                CourseRegionFilter(
                    includePrefixes = listOf(JEONNAM_GWANGJU_SIDO),
                    excludePrefixes = GWANGJU_SIGUNGU_PREFIXES,
                )
            }

            else -> {
                require(
                    region.length in PREFIX_LENGTHS && region.all { it.isDigit() },
                ) { "지역 코드 형식이 올바르지 않습니다: $region" }
                require(areaQueryUseCase.existsAreaWithPrefix(region)) { "알 수 없는 지역입니다: $region" }
                CourseRegionFilter(includePrefixes = listOf(region))
            }
        }

    private companion object {
        val PREFIX_LENGTHS = setOf(2, 4, 5)
        const val JEONNAM_GWANGJU_SIDO = "12"
        const val GWANGJU_VIRTUAL_SIDO = "29"
        const val JEONNAM_VIRTUAL_SIDO = "46"

        /** 통합 시도 안의 광주 구: 동구·서구·남구·북구·광산구. */
        val GWANGJU_SIGUNGU_PREFIXES = listOf("12210", "12240", "12270", "12300", "12330")
    }
}
