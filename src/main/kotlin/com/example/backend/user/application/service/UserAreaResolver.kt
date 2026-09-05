package com.example.backend.user.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.AreaErrorCode
import com.example.backend.user.application.port.inbound.dto.UserAreaResult
import org.springframework.stereotype.Component

/**
 * 관심 지역 코드 공통 로직 — 회원가입·프로필 수정이 함께 쓴다.
 *
 * 클라이언트는 지역 검색 응답의 prefix 를 그대로 보낸다: 읍면동은 10자리, 시군구는 5자리.
 * 저장은 10자리 정규형으로 통일한다(시군구는 뒤 5자리 0 패딩 — AreaQueryUseCase 의 시군구 레벨 표현).
 */
@Component
class UserAreaResolver(
    private val areaQueryUseCase: AreaQueryUseCase,
) {
    /** 코드를 10자리로 정규화·중복 제거하고 area 마스터에 실재하는지 검증한다(미존재·비활성이면 거절). */
    fun normalizeAndValidate(areaCodes: List<String>): List<String> {
        val normalized = areaCodes.map(::normalize).distinct()
        val missing = normalized.filter { areaQueryUseCase.findAreaByCode(it) == null }
        if (missing.isNotEmpty()) {
            throw BusinessException(AreaErrorCode.AREA_NOT_FOUND, "존재하지 않는 지역 코드가 포함되어 있습니다: codes=$missing")
        }
        return normalized
    }

    /** 저장된 코드 목록을 표시용(코드+이름)으로 푼다. 저장 후 폐지(비활성)된 코드는 숨긴다. */
    fun resolve(areaCodes: List<String>): List<UserAreaResult> =
        areaCodes.mapNotNull { code ->
            areaQueryUseCase.findAreaByCode(code)?.let { UserAreaResult(code = code, name = it.shortName) }
        }

    private fun normalize(code: String): String =
        if (code.length == SIGUNGU_PREFIX_LENGTH) code + SIGUNGU_LEVEL_SUFFIX else code

    private companion object {
        const val SIGUNGU_PREFIX_LENGTH = 5
        const val SIGUNGU_LEVEL_SUFFIX = "00000"
    }
}
