package com.example.backend.area.adapter.outbound.persistence

import com.example.backend.area.adapter.outbound.persistence.exposed.repository.AreaRepository
import com.example.backend.support.IntegrationTestBase
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * area 테이블 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 * generated column(sigungu_code) 자동 채움, area_code prefix 검색, 리포지토리 매핑을 검증한다.
 * 각 테스트는 transaction { ... rollback() } 으로 격리한다(픽스처 오염 없음).
 * 픽스처 코드는 실존하지 않는 가상 시도(99) 를 쓴다 — V13 법정동 시드와의 PK 충돌 방지.
 */
class AreaPersistenceTest
    @Autowired
    constructor(
        private val areaRepository: AreaRepository,
    ) : IntegrationTestBase() {
        @Test
        fun `sigungu_code 를 명시하지 않아도 GENERATED 컬럼이 code 앞 5자리로 자동 채워진다`() {
            transaction {
                // insert 블록에 sigungu_code 를 명시하지 않는다(databaseGenerated).
                AreaTable.insert {
                    it[code] = "9968010100"
                    it[sidoName] = "서울특별시"
                    it[sigunguName] = "강남구"
                    it[dongName] = "역삼동"
                    it[isActive] = true
                }

                val row =
                    AreaTable
                        .select(AreaTable.sigunguCode)
                        .where { AreaTable.code eq "9968010100" }
                        .single()

                assertEquals("99680", row[AreaTable.sigunguCode])
                rollback()
            }
        }

        @Test
        fun `area_code prefix 검색(LIKE 99680 퍼센트)은 같은 시군구의 읍면동만 반환한다`() {
            transaction {
                insertArea("9968010100", "강남구", "역삼동")
                insertArea("9968010500", "강남구", "삼성동")
                insertArea("9965010800", "서초구", "서초동") // 다른 시군구(99650)

                val gangnamCodes =
                    AreaTable
                        .selectAll()
                        .where { AreaTable.code like "99680%" }
                        .map { it[AreaTable.code] }

                assertTrue("9968010100" in gangnamCodes)
                assertTrue("9968010500" in gangnamCodes)
                assertFalse("9965010800" in gangnamCodes) // 서초구는 제외
                rollback()
            }
        }

        @Test
        fun `findAllActive 는 활성 지역만 도메인으로 매핑한다`() {
            transaction {
                insertArea("9968010100", "강남구", "역삼동", active = true)
                insertArea("9968010900", "강남구", "폐지동", active = false)

                val active = areaRepository.findAllActive()
                val byCode = active.associateBy { it.code.value }

                val yeoksam = byCode["9968010100"]
                assertTrue(yeoksam != null)
                assertEquals("서울특별시", yeoksam!!.sidoName)
                assertEquals("강남구", yeoksam.sigunguName)
                assertEquals("역삼동", yeoksam.dongName)
                assertEquals("99680", yeoksam.code.sigunguCode)
                assertFalse("9968010900" in byCode.keys) // 비활성 제외
                rollback()
            }
        }

        private fun insertArea(
            code: String,
            sigungu: String,
            dong: String,
            active: Boolean = true,
        ) {
            AreaTable.insert {
                it[AreaTable.code] = code
                it[sidoName] = "서울특별시"
                it[sigunguName] = sigungu
                it[dongName] = dong
                it[isActive] = active
            }
        }
    }
