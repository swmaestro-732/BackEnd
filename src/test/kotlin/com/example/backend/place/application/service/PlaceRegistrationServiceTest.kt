package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.place.application.port.inbound.dto.RegisterPlaceCommand
import com.example.backend.place.application.port.outbound.AreaCodeLookupPort
import com.example.backend.place.application.port.outbound.PlacePersistencePort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.support.TransactionOperations

class PlaceRegistrationServiceTest {
    private val persistence = FakePersistence()
    private val areaRequests = mutableListOf<Coordinate>()
    private val areas =
        object : AreaCodeLookupPort {
            override fun findLegalDongCode(coordinate: Coordinate): String {
                areaRequests += coordinate
                return "1120011400"
            }
        }
    private val events = mutableListOf<Any>()
    private val service =
        PlaceRegistrationService(
            persistence,
            areas,
            TransactionOperations.withoutTransaction(),
            ApplicationEventPublisher { events += it },
        )

    @ParameterizedTest
    @ValueSource(strings = ["https://cdn.example.com/existing.jpg", "  "])
    fun `기존 활성 장소는 새 정보로 덮어쓰지 않고 저장된 ID와 좌표를 반환한다`(imageUrl: String) {
        val existing = existingPlace(imageUrl)
        persistence.rows["k1"] = existing

        val result = service.register(listOf(command())).single()

        assertEquals(42L, result.id)
        assertEquals(Coordinate(37.6, 127.1), result.coordinate)
        assertEquals(existing.imageUrl, result.imageUrl)
        assertEquals(existing, persistence.rows["k1"])
        assertTrue(persistence.inserted.isEmpty())
        assertTrue(areaRequests.isEmpty())
    }

    @ParameterizedTest
    @NullAndEmptySource
    fun `기존 장소의 없는 이미지만 채우고 응답과 색인 이벤트에도 반영한다`(imageUrl: String?) {
        val existing = existingPlace(imageUrl)
        persistence.rows["k1"] = existing

        val result = service.register(listOf(command())).single()

        val saved = persistence.rows.getValue("k1")
        assertEquals(existing.persisted(42L, "https://cdn.example.com/new.jpg"), saved)
        assertEquals(42L, result.id)
        assertEquals("https://cdn.example.com/new.jpg", result.imageUrl)
        assertEquals(listOf(saved), events.filterIsInstance<PlacesSavedEvent>().single().places)
        assertTrue(persistence.inserted.isEmpty())
        assertTrue(areaRequests.isEmpty())
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
        strings = ["  ", "http://cdn.example.com/a.jpg", "https://", "https://bad host/a.jpg", "javascript:alert(1)"],
    )
    fun `등록 이미지가 없거나 유효한 https가 아니면 기존 장소를 갱신하지 않는다`(imageUrl: String?) {
        val existing = existingPlace(null)
        persistence.rows["k1"] = existing

        val result = service.register(listOf(command().copy(imageUrl = imageUrl))).single()

        assertEquals(existing, persistence.rows["k1"])
        assertNull(result.imageUrl)
        assertEquals(listOf(existing), events.filterIsInstance<PlacesSavedEvent>().single().places)
    }

    @Test
    fun `신규 장소는 좌표로 지역코드를 구하고 전달받은 설명과 이미지를 저장한다`() {
        val result = service.register(listOf(command())).single()

        val saved = persistence.rows.getValue("k1")
        assertEquals("새 이름", saved.name)
        assertEquals("새 설명", saved.description)
        assertEquals("https://cdn.example.com/new.jpg", saved.imageUrl)
        assertEquals("1120011400", saved.areaCode)
        assertEquals(Coordinate(37.5, 127.0), saved.location)
        assertEquals("서울 성동구", saved.address)
        assertEquals(PlaceCategory.CAFE, saved.category)
        assertEquals(listOf(Coordinate(37.5, 127.0)), areaRequests)
        assertEquals(saved.id, result.id)
        assertEquals("k1", result.kakaoPlaceId)
        assertEquals("CAFE", result.category)
        assertEquals(listOf(saved), events.filterIsInstance<PlacesSavedEvent>().single().places)
    }

    @Test
    fun `같은 카카오 ID가 반복돼도 신규 장소와 지역 조회는 한 번만 만든다`() {
        val result = service.register(listOf(command(), command().copy(name = "중복 이름")))

        assertEquals(1, result.size)
        assertEquals(1, persistence.inserted.size)
        assertEquals("새 이름", persistence.inserted.single().name)
        assertEquals(listOf(Coordinate(37.5, 127.0)), areaRequests)
    }

    @Test
    fun `삽입 경합으로 내 저장이 무시돼도 재조회한 다른 요청의 장소를 반환한다`() {
        val winner =
            Place
                .create(
                    name = "다른 요청이 저장한 장소",
                    description = "먼저 저장된 설명",
                    category = PlaceCategory.CAFE,
                    location = Coordinate(37.6, 127.1),
                    address = "서울 성동구",
                    areaCode = "1120011400",
                    imageUrl = "https://cdn.example.com/winner.jpg",
                    kakaoPlaceId = "k1",
                ).persisted(88L)
        persistence.concurrentInsert = winner

        val result = service.register(listOf(command())).single()

        assertEquals(88L, result.id)
        assertEquals(Coordinate(37.6, 127.1), result.coordinate)
        assertEquals(listOf(winner), events.filterIsInstance<PlacesSavedEvent>().single().places)
    }

    @Test
    fun `삽입 경합에서 먼저 저장된 장소에 이미지가 없으면 채운다`() {
        persistence.concurrentInsert = existingPlace(null).persisted(88L)

        val result = service.register(listOf(command())).single()

        assertEquals(88L, result.id)
        assertEquals("https://cdn.example.com/new.jpg", result.imageUrl)
        assertEquals(result.imageUrl, persistence.rows.getValue("k1").imageUrl)
        assertEquals(
            result.imageUrl,
            events
                .filterIsInstance<PlacesSavedEvent>()
                .single()
                .places
                .single()
                .imageUrl,
        )
    }

    @Test
    fun `이름 주소 또는 카카오 ID가 빈 신규 장소는 제외한다`() {
        val result =
            service.register(
                listOf(
                    command(),
                    command().copy(kakaoPlaceId = "k2", name = "  "),
                    command().copy(kakaoPlaceId = "k3", address = ""),
                    command().copy(kakaoPlaceId = "  "),
                ),
            )

        assertEquals(listOf("k1"), result.map { it.kakaoPlaceId })
        assertEquals(listOf("k1"), persistence.inserted.map { it.kakaoPlaceId })
        assertEquals(listOf(Coordinate(37.5, 127.0)), areaRequests)
    }

    @Test
    fun `빈 설명과 이미지는 없는 값으로 저장한다`() {
        service.register(listOf(command().copy(description = "  ", imageUrl = "")))

        val saved = persistence.rows.getValue("k1")
        assertNull(saved.description)
        assertNull(saved.imageUrl)
    }

    @Test
    fun `알 수 없는 카테고리는 조회나 등록 전에 INVALID_INPUT으로 거절한다`() {
        val error =
            assertThrows(BusinessException::class.java) {
                service.register(listOf(command(), command().copy(kakaoPlaceId = "k2", category = "UNKNOWN")))
            }

        assertEquals(CommonErrorCode.INVALID_INPUT, error.errorCode)
        assertEquals("unknown category: UNKNOWN", error.message)
        assertTrue(persistence.rows.isEmpty())
        assertTrue(areaRequests.isEmpty())
        assertTrue(events.isEmpty())
    }

    private fun existingPlace(imageUrl: String?) =
        Place
            .create(
                name = "기존 이름",
                description = "기존 설명",
                category = PlaceCategory.CAFE,
                location = Coordinate(37.6, 127.1),
                address = "기존 주소",
                areaCode = "1111010100",
                imageUrl = imageUrl,
                kakaoPlaceId = "k1",
            ).persisted(42L)

    private fun command() =
        RegisterPlaceCommand(
            kakaoPlaceId = "k1",
            name = "새 이름",
            category = "CAFE",
            coordinate = Coordinate(37.5, 127.0),
            address = "서울 성동구",
            description = "새 설명",
            imageUrl = "https://cdn.example.com/new.jpg",
        )

    private class FakePersistence : PlacePersistencePort {
        val rows = mutableMapOf<String, Place>()
        val inserted = mutableListOf<Place>()
        var concurrentInsert: Place? = null
        private var sequence = 0L

        override fun findByKakaoIds(kakaoIds: List<String>): List<Place> = kakaoIds.distinct().mapNotNull { rows[it] }

        override fun insertIgnoringConflicts(places: List<Place>) {
            concurrentInsert?.let { rows[it.kakaoPlaceId!!] = it }
            inserted += places
            places.forEach { place ->
                rows.putIfAbsent(place.kakaoPlaceId!!, place.persisted(++sequence))
            }
        }

        override fun fillImageIfEmpty(
            placeId: Long,
            imageUrl: String,
        ) {
            val place = rows.values.firstOrNull { it.id == placeId } ?: return
            if (place.imageUrl.isNullOrEmpty()) rows[place.kakaoPlaceId!!] = place.persisted(placeId, imageUrl)
        }

        override fun findForIndex(
            afterId: Long?,
            limit: Int,
        ): List<Place> = error("재색인은 등록 서비스의 호출 대상이 아닙니다.")
    }
}

private fun Place.persisted(
    id: Long,
    imageUrl: String? = this.imageUrl,
): Place =
    Place.reconstitute(
        id = id,
        status = status,
        name = name,
        description = description,
        category = category,
        location = location,
        address = address,
        areaCode = areaCode,
        imageUrl = imageUrl,
        businessStatus = businessStatus,
        kakaoPlaceId = kakaoPlaceId,
        createdAt = null,
        updatedAt = null,
        deletedAt = null,
    )
