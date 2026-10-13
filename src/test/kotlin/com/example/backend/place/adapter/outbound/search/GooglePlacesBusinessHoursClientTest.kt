package com.example.backend.place.adapter.outbound.search

import com.example.backend.bootstrap.config.GooglePlacesProperties
import com.example.backend.common.geo.Coordinate
import kotlinx.datetime.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

/**
 * [GooglePlacesBusinessHoursClient] — 텍스트 검색 한 번으로 좌표·영업시간을 받는 호출을 목으로 세우고,
 * period → 요일별 행 변환과 fail-soft(null)·미매칭(빈 목록) 구분을 검증한다.
 */
class GooglePlacesBusinessHoursClientTest {
    private val builder = RestClient.builder().baseUrl("https://places.googleapis.com")
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client =
        GooglePlacesBusinessHoursClient(
            googlePlacesRestClient = builder.build(),
            properties = GooglePlacesProperties(apiKey = "api-key"),
        )
    private val seoul = Coordinate(latitude = 37.5432, longitude = 127.0561)

    /** 검색 응답 한 건을 세운다. 호출은 언제나 한 번뿐이다(상세 조회 없음). */
    private fun expectSearch(body: String) {
        server
            .expect(requestTo(containsString("/v1/places:searchText")))
            .andExpect(header("X-Goog-Api-Key", "api-key"))
            .andExpect(header("X-Goog-FieldMask", "places.id,places.location,places.regularOpeningHours"))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))
    }

    /** 서울 좌표의 장소 한 건에 주어진 영업시간 조각을 붙인 응답. */
    private fun placeBody(openingHours: String): String =
        """
        {"places":[{"id":"google-1","location":{"latitude":37.5432,"longitude":127.0561}$openingHours}]}
        """.trimIndent()

    private fun periods(vararg json: String): String =
        ""","regularOpeningHours":{"periods":[${json.joinToString(",")}]}"""

    @Test
    fun `period 를 요일별 행으로 바꾸고 없는 요일은 휴무로 채운다`() {
        expectSearch(
            placeBody(
                periods(
                    """{"open":{"day":1,"hour":9,"minute":0},"close":{"day":1,"hour":22,"minute":0}}""",
                    """{"open":{"day":0,"hour":11,"minute":30},"close":{"day":0,"hour":20,"minute":0}}""",
                ),
            ),
        )

        val hours = client.findBusinessHours("콤포트 성수", "서울 성동구 서울숲2길 3", seoul)

        server.verify()
        assertThat(hours).hasSize(7)
        val byDay = hours!!.associateBy { it.dayOfWeek }
        assertThat(byDay[1]!!.openTime).isEqualTo(LocalTime(9, 0))
        assertThat(byDay[1]!!.closeTime).isEqualTo(LocalTime(22, 0))
        // 구글 day=0(일) → ISO 7
        assertThat(byDay[7]!!.openTime).isEqualTo(LocalTime(11, 30))
        assertThat(byDay[7]!!.closeTime).isEqualTo(LocalTime(20, 0))
        // period 에 없는 요일은 휴무
        assertThat(byDay[3]!!.openTime).isNull()
        assertThat(byDay[3]!!.closeTime).isNull()
        // 구간이 하나뿐인 날은 쉬는 시간이 없다
        assertThat(byDay[1]!!.breakStart).isNull()
        assertThat(byDay[1]!!.breakEnd).isNull()
    }

    @Test
    fun `같은 요일에 period 가 둘이면 그 사이를 쉬는 시간으로 담는다`() {
        expectSearch(
            placeBody(
                periods(
                    """{"open":{"day":2,"hour":11,"minute":0},"close":{"day":2,"hour":15,"minute":0}}""",
                    """{"open":{"day":2,"hour":17,"minute":0},"close":{"day":2,"hour":21,"minute":0}}""",
                ),
            ),
        )

        val tuesday = client.findBusinessHours("브레이크타임 식당", "서울 어딘가", seoul)!!.single { it.dayOfWeek == 2 }

        server.verify()
        assertThat(tuesday.openTime).isEqualTo(LocalTime(11, 0))
        assertThat(tuesday.closeTime).isEqualTo(LocalTime(21, 0))
        assertThat(tuesday.breakStart).isEqualTo(LocalTime(15, 0))
        assertThat(tuesday.breakEnd).isEqualTo(LocalTime(17, 0))
    }

    @Test
    fun `자정을 넘기는 영업은 폐점이 개점보다 이른 값으로 담긴다`() {
        expectSearch(
            placeBody(periods("""{"open":{"day":5,"hour":18,"minute":0},"close":{"day":6,"hour":2,"minute":0}}""")),
        )

        val friday = client.findBusinessHours("심야 바", "서울 어딘가", seoul)!!.single { it.dayOfWeek == 5 }

        server.verify()
        assertThat(friday.openTime).isEqualTo(LocalTime(18, 0))
        assertThat(friday.closeTime).isEqualTo(LocalTime(2, 0))
    }

    @Test
    fun `close 없는 단일 period 는 24시간 영업으로 본다`() {
        expectSearch(placeBody(periods("""{"open":{"day":0,"hour":0,"minute":0}}""")))

        val hours = client.findBusinessHours("24시 편의점", "서울 어딘가", seoul)

        server.verify()
        assertThat(hours).hasSize(7)
        assertThat(hours!!.map { it.openTime }).containsOnly(LocalTime(0, 0))
        assertThat(hours.map { it.closeTime }).containsOnly(LocalTime(23, 59))
    }

    @Test
    fun `좌표가 멀면 다른 장소로 보고 빈 목록을 반환한다`() {
        // 부산 좌표 — 요청한 서울 좌표에서 수백 km
        expectSearch(
            """
            {"places":[{"id":"google-1","location":{"latitude":35.1796,"longitude":129.0756},
             "regularOpeningHours":{"periods":[{"open":{"day":1,"hour":9},"close":{"day":1,"hour":18}}]}}]}
            """.trimIndent(),
        )

        val hours = client.findBusinessHours("동명이소 카페", "서울 성동구", seoul)

        server.verify()
        assertThat(hours).isEmpty()
    }

    @Test
    fun `검색 결과가 없으면 빈 목록을 반환한다(동기화 완료로 기록)`() {
        expectSearch("""{"places":[]}""")

        val hours = client.findBusinessHours("없는 장소", "서울 어딘가", seoul)

        server.verify()
        assertThat(hours).isEmpty()
    }

    @Test
    fun `영업시간 정보가 없으면 빈 목록을 반환한다`() {
        expectSearch(placeBody(""))

        val hours = client.findBusinessHours("영업시간 미등록", "서울 어딘가", seoul)

        server.verify()
        assertThat(hours).isEmpty()
    }

    @Test
    fun `호출이 실패하면 null 을 반환해 다음 동기화에서 재시도된다`() {
        server.expect(requestTo(containsString("/v1/places:searchText"))).andRespond(withServerError())

        val hours = client.findBusinessHours("콤포트 성수", "서울 어딘가", seoul)

        server.verify()
        assertThat(hours).isNull()
    }

    @Test
    fun `키가 비면 호출하지 않고 null 을 반환한다`() {
        val keyless =
            GooglePlacesBusinessHoursClient(
                googlePlacesRestClient = builder.build(),
                properties = GooglePlacesProperties(apiKey = ""),
            )

        assertThat(keyless.findBusinessHours("콤포트 성수", "서울 어딘가", seoul)).isNull()
        server.verify() // 기대한 요청 없음 = 호출 안 함
    }
}
