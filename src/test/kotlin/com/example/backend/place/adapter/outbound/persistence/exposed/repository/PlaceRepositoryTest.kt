package com.example.backend.place.adapter.outbound.persistence.exposed.repository

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceTable
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.time.Clock

/**
 * [PlaceRepository] 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * insert(ignore)→재조회 왕복, id/kakaoId 조회, 이름 LIKE 검색(와일드카드 이스케이프·커서 seek),
 * 재색인 페이징, 소프트 삭제 제외를 검증한다. 각 테스트는 transaction { ... rollback() } 로 격리한다.
 */
class PlaceRepositoryTest
    @Autowired
    constructor(
        private val repository: PlaceRepository,
    ) : IntegrationTestBase() {
        @Test
        fun `insertIgnoringConflicts 는 kakaoId 없는 항목을 빼고 저장하고 findByKakaoIds 로 되읽힌다`() {
            transaction {
                repository.insertIgnoringConflicts(
                    listOf(
                        place("스타벅스 강남", kakaoId = "kakao-1"),
                        place("투썸 역삼", kakaoId = "kakao-2"),
                        place("이름만있고카카오없음", kakaoId = null), // dedup 키 없음 → 저장 안 됨
                        place("공백카카오", kakaoId = "   "), // blank → 저장 안 됨
                    ),
                )

                val found = repository.findByKakaoIds(listOf("kakao-1", "kakao-2", "kakao-없음"))
                assertThat(found.map { it.name })
                    .containsExactlyInAnyOrder("스타벅스 강남", "투썸 역삼")
                // 저장된 엔티티가 도메인 값을 온전히 담는지(카테고리·좌표 매핑)
                val one = found.first { it.kakaoPlaceId == "kakao-1" }
                assertThat(one.category).isEqualTo(PlaceCategory.CAFE)
                assertThat(one.name).isEqualTo("스타벅스 강남")

                // blank·null kakaoId 는 아예 삽입되지 않았다
                assertThat(repository.findByKakaoIds(listOf("kakao-2")).single().name).isEqualTo("투썸 역삼")
                rollback()
            }
        }

        @Test
        fun `findByKakaoIds 는 공백을 걸러내고 전부 공백이면 빈 목록을 즉시 반환한다`() {
            transaction {
                assertThat(repository.findByKakaoIds(emptyList())).isEmpty()
                assertThat(repository.findByKakaoIds(listOf("", "   "))).isEmpty()
                rollback()
            }
        }

        @Test
        fun `findByIds 는 요청 id 중 살아있는 장소만 반환한다(소프트 삭제 제외)`() {
            transaction {
                repository.insertIgnoringConflicts(
                    listOf(place("살아있음", "k-a"), place("삭제될것", "k-b")),
                )
                val byKakao = repository.findByKakaoIds(listOf("k-a", "k-b")).associateBy { it.kakaoPlaceId }
                val aliveId = byKakao.getValue("k-a").id.value
                val deletedId = byKakao.getValue("k-b").id.value
                softDelete(deletedId)

                val found = repository.findByIds(listOf(aliveId, deletedId))
                assertThat(found.map { it.id.value }).containsExactly(aliveId)
                rollback()
            }
        }

        @Test
        fun `searchByName 은 부분일치를 id 오름차순 limit 개로 주고 커서 이후부터 이어준다`() {
            transaction {
                repository.insertIgnoringConflicts(
                    listOf(
                        place("한강공원 반포", "s-1"),
                        place("한강공원 뚝섬", "s-2"),
                        place("한강공원 여의도", "s-3"),
                        place("남산타워", "s-4"), // 매치 안 됨
                    ),
                )

                val firstPage = repository.searchByName("한강공원", cursor = null, limit = 2)
                assertThat(firstPage.map { it.name }).containsExactly("한강공원 반포", "한강공원 뚝섬")
                assertThat(firstPage.map { it.id.value }).isSorted

                val secondPage = repository.searchByName("한강공원", cursor = firstPage.last().id.value, limit = 2)
                assertThat(secondPage.map { it.name }).containsExactly("한강공원 여의도")

                assertThat(repository.countByName("한강공원")).isEqualTo(3L)
                rollback()
            }
        }

        @Test
        fun `searchByName 은 LIKE 와일드카드를 리터럴로 취급한다`() {
            transaction {
                repository.insertIgnoringConflicts(
                    listOf(
                        place("50% 할인마트", "w-1"), // 리터럴 '%' 포함
                        place("아무카페", "w-2"), // '%' 없음
                    ),
                )

                // '%' 를 이스케이프하지 않으면 LIKE '%%%' 로 전부 매칭될 것 — 리터럴이면 '50%' 만 매칭
                val hits = repository.searchByName("50%", cursor = null, limit = 10)
                assertThat(hits.map { it.name }).containsExactly("50% 할인마트")
                assertThat(repository.countByName("50%")).isEqualTo(1L)
                rollback()
            }
        }

        @Test
        fun `findForIndex 는 afterId 다음부터 id 오름차순으로 살아있는 장소를 페이징한다`() {
            transaction {
                repository.insertIgnoringConflicts(
                    listOf(place("색인1", "i-1"), place("색인2", "i-2"), place("색인3", "i-3")),
                )
                // 한 batchInsert 라 id 가 연속 → 방금 넣은 3건이 테이블에서 가장 큰 id(가장 최신)다.
                val all = repository.findByKakaoIds(listOf("i-1", "i-2", "i-3")).sortedBy { it.id.value }
                softDelete(all[1].id.value) // 가운데 하나 소프트 삭제

                // afterId 로 방금 넣은 첫 행 뒤부터 → 삭제된 all[1] 은 건너뛰고 all[2] 만(그 뒤 행은 없음).
                val afterFirst = repository.findForIndex(afterId = all[0].id.value, limit = 10)
                assertThat(afterFirst.map { it.id.value }).containsExactly(all[2].id.value)

                // afterId 바로 이전부터 시작하면 살아있는 all[0]·all[2] 를 오름차순으로 준다(삭제된 all[1] 제외).
                val fromBeforeFirst = repository.findForIndex(afterId = all[0].id.value - 1, limit = 10)
                assertThat(fromBeforeFirst.map { it.id.value }).containsExactly(all[0].id.value, all[2].id.value)
                rollback()
            }
        }

        private fun place(
            name: String,
            kakaoId: String?,
        ): Place =
            Place.create(
                name = name,
                description = null,
                category = PlaceCategory.CAFE,
                location = Coordinate(latitude = 37.5, longitude = 127.0),
                address = "서울시 $name",
                imageUrl = null,
                kakaoPlaceId = kakaoId,
            )

        private fun softDelete(placeId: Long) {
            PlaceTable.update({ PlaceTable.id eq placeId }) {
                it[deletedAt] = Clock.System.now()
            }
        }
    }
