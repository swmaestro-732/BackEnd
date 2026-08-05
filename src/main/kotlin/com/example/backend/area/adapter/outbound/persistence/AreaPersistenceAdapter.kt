package com.example.backend.area.adapter.outbound.persistence

import com.example.backend.area.adapter.outbound.persistence.exposed.repository.AreaRepository
import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.area.application.port.outbound.AreaDirectoryPort
import com.example.backend.area.domain.model.Area
import com.example.backend.area.domain.model.AreaLevel
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [AreaDirectoryPort] 를 구현한다.
 *
 * 전국 읍면동이 5천 건 이하이고 거의 바뀌지 않으므로 활성 지역 전체를 한 번 적재하고,
 * 시군구 항목은 읍면동 코드 prefix(앞 5자리) groupBy 로 파생한다(별도 저장 없음 — 원본과 어긋날 여지 없음).
 * 캐시는 프로세스 수명 동안 유지된다(시드 갱신 반영은 재기동으로 한다).
 */
@Component
class AreaPersistenceAdapter(
    private val areaRepository: AreaRepository,
) : AreaDirectoryPort {
    private val activeAreas: List<Area> by lazy { areaRepository.findAllActive() }

    /** 5자리 → 시/군/구(읍면동에서 파생). 세종처럼 sigunguName 이 null 인 그룹은 제외한다. */
    private val sigunguMap: Map<String, AreaDescriptor> by lazy {
        activeAreas
            .filter { it.sigunguName != null }
            .groupBy { it.code.sigunguCode }
            // 같은 시군구 그룹의 이름은 모두 동일하므로 아무 원소(first)에서 읽으면 된다.
            .mapValues { (prefix, group) ->
                val sample = group.first()
                AreaDescriptor(
                    prefix = prefix,
                    // 행정구를 가진 시(예: "고양시 덕양구")는 sigunguName 전체를 그대로 short 이름으로 쓴다.
                    shortName = sample.sigunguName!!,
                    fullName = listOfNotNull(sample.sidoName, sample.sigunguName).joinToString(" "),
                    level = AreaLevel.SIGUNGU,
                )
            }
    }

    override fun search(keyword: String): List<AreaDescriptor> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return emptyList()
        return (sigunguMap.values.asSequence() + activeAreas.asSequence().map { it.toDescriptor() })
            .filter { it.shortName.contains(trimmed) || it.fullName.contains(trimmed) }
            .sortedBy { it.prefix }
            .take(SEARCH_LIMIT)
            .toList()
    }

    /** 읍면동 [Area] → 읍면동 레벨 [AreaDescriptor]. fullName 은 null(세종 시군구)을 건너뛰어 조합한다. */
    private fun Area.toDescriptor(): AreaDescriptor =
        AreaDescriptor(
            prefix = code.value,
            shortName = dongName,
            fullName = listOfNotNull(sidoName, sigunguName, dongName).joinToString(" "),
            level = AreaLevel.DONG,
        )

    private companion object {
        const val SEARCH_LIMIT = 20
    }
}
