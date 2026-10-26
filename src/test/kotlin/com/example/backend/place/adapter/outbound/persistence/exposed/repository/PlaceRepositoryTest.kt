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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * [PlaceRepository] 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * insert(ignore)→재조회 왕복, id/kakaoId 조회, 재색인 페이징, 소프트 삭제 제외를 검증한다. 각 테스트는 transaction { ... rollback() } 로 격리한다.
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
        fun `findForIndex 는 afterId 다음부터 id 오름차순으로 살아있는 장소를 페이징한다`() {
            transaction {
                repository.insertIgnoringConflicts(
                    listOf(place("색인1", "i-1"), place("색인2", "i-2"), place("색인3", "i-3")),
                )
                // 한 batchInsert 라 id 가 연속이다.
                val all = repository.findByKakaoIds(listOf("i-1", "i-2", "i-3")).sortedBy { it.id.value }
                softDelete(all[1].id.value) // 가운데 하나 소프트 삭제

                // 결과는 이 테스트가 넣은 3건으로 좁혀 본다 — @Sql 픽스처가 명시 id(예: place-review-fixture 의 601)로
                // 심은 행이 커밋된 채 남아 있어, "방금 넣은 3건이 테이블에서 가장 큰 id" 라고 가정할 수 없다.
                val mine = all.map { it.id.value }.toSet()

                // afterId 로 방금 넣은 첫 행 뒤부터 → 삭제된 all[1] 은 건너뛰고 all[2] 만.
                val afterFirst = repository.findForIndex(afterId = all[0].id.value, limit = 10)
                assertThat(afterFirst.map { it.id.value }.filter { it in mine })
                    .containsExactly(all[2].id.value)

                // afterId 바로 이전부터 시작하면 살아있는 all[0]·all[2] 를 오름차순으로 준다(삭제된 all[1] 제외).
                val fromBeforeFirst = repository.findForIndex(afterId = all[0].id.value - 1, limit = 10)
                assertThat(fromBeforeFirst.map { it.id.value }.filter { it in mine })
                    .containsExactly(all[0].id.value, all[2].id.value)
                rollback()
            }
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = ["https://cdn.example.com/existing.jpg", "  "])
        fun `이미지가 null이나 빈 문자열일 때만 이미지와 수정 시각을 갱신한다`(imageUrl: String?) {
            transaction {
                repository.insertIgnoringConflicts(
                    listOf(place("이미지 대상", "image-target", imageUrl), place("다른 장소", "image-other")),
                )
                val targetId =
                    repository
                        .findByKakaoIds(listOf("image-target"))
                        .single()
                        .id.value
                val previous = Instant.parse("2020-01-01T00:00:00Z")
                PlaceTable.update({ PlaceTable.id eq targetId }) { it[updatedAt] = previous }

                repository.fillImageIfEmpty(targetId, "https://cdn.example.com/new.jpg")

                val saved = repository.findByKakaoIds(listOf("image-target")).single()
                assertThat(saved.imageUrl)
                    .isEqualTo(if (imageUrl.isNullOrEmpty()) "https://cdn.example.com/new.jpg" else imageUrl)
                if (imageUrl.isNullOrEmpty()) {
                    assertThat(saved.updatedAt).isGreaterThan(previous)
                } else {
                    assertThat(saved.updatedAt).isEqualTo(previous)
                }
                assertThat(repository.findByKakaoIds(listOf("image-other")).single().imageUrl).isNull()
                val savedImage = saved.imageUrl
                val savedUpdatedAt = saved.updatedAt

                repository.fillImageIfEmpty(targetId, "https://cdn.example.com/replacement.jpg")

                val unchanged = repository.findByKakaoIds(listOf("image-target")).single()
                assertThat(unchanged.imageUrl).isEqualTo(savedImage)
                assertThat(unchanged.updatedAt).isEqualTo(savedUpdatedAt)
                rollback()
            }
        }

        private fun place(
            name: String,
            kakaoId: String?,
            imageUrl: String? = null,
        ): Place =
            Place.create(
                name = name,
                description = null,
                category = PlaceCategory.CAFE,
                location = Coordinate(latitude = 37.5, longitude = 127.0),
                address = "서울시 $name",
                imageUrl = imageUrl,
                kakaoPlaceId = kakaoId,
            )

        private fun softDelete(placeId: Long) {
            PlaceTable.update({ PlaceTable.id eq placeId }) {
                it[deletedAt] = Clock.System.now()
            }
        }
    }
