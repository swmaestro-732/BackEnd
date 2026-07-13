package com.example.backend.place.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@Sql(scripts = ["/sql/place-detail-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlaceControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `장소 상세를 DB에서 조회해 명세 형태로 내려준다`() {
            mockMvc
                .perform(get("/api/v1/places/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.place.id").value(1))
                .andExpect(jsonPath("$.data.place.name").value("어니언 성수"))
                .andExpect(jsonPath("$.data.place.categories[0]").value("카페"))
                .andExpect(jsonPath("$.data.place.imageUrls[0]").value("https://cdn.example.com/places/1/1.jpg"))
                .andExpect(jsonPath("$.data.place.address").value("서울 성동구 아차산로 100"))
                .andExpect(jsonPath("$.data.place.location.latitude").value(37.5446))
                .andExpect(jsonPath("$.data.place.location.longitude").value(127.0559))
                .andExpect(jsonPath("$.data.place.openStatus").value("OPEN"))
                .andExpect(jsonPath("$.data.place.openingHoursText").value("매일 11:00 – 21:00"))
                .andExpect(jsonPath("$.data.place.reviewSummary.averageRating").value(4.7))
                .andExpect(jsonPath("$.data.place.reviewSummary.totalCount").value(3))
                .andExpect(jsonPath("$.data.place.reviewSummary.reviews.length()").value(2))
                .andExpect(jsonPath("$.data.place.reviewSummary.reviews[0].author.id").value(1))
                .andExpect(jsonPath("$.data.place.reviewSummary.reviews[0].author.nickname").value("현우님"))
                .andExpect(jsonPath("$.data.place.reviewSummary.reviews[0].relativeTime").value("3일 전"))
                .andExpect(
                    jsonPath(
                        "$.data.place.reviewSummary.reviews[0].photoUrls[0]",
                    ).value("https://cdn.example.com/reviews/1/1.jpg"),
                ).andExpect(jsonPath("$.data.place.viewer.hasSaved").value(false))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
        }

        @Test
        fun `없는 장소면 4040 에러`() {
            mockMvc
                .perform(get("/api/v1/places/999999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
                .andExpect(jsonPath("$.message").value("장소를 찾을 수 없습니다: id=999999"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        fun `placeId 형식이 잘못되면 4001 에러`() {
            mockMvc
                .perform(get("/api/v1/places/abc"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.data").doesNotExist())
        }
    }
