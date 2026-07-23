package com.example.backend.bff.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 장소 상세 화면 조합 BFF(`GET /service/v1/places/{placeId}`) 목업 검증.
 * 목 데이터라 DB 픽스처가 필요 없다(컨트롤러 인라인 페이로드).
 */
@AutoConfigureMockMvc
class PlaceDetailScreenControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `장소 상세 화면을 조합해 내려준다`() {
            mockMvc
                .perform(get("/service/v1/places/101"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.place.id").value(101))
                .andExpect(jsonPath("$.data.place.name").value("어니언 성수"))
                .andExpect(jsonPath("$.data.place.categories[0]").value("카페"))
                .andExpect(jsonPath("$.data.place.location.latitude").value(37.5446))
                .andExpect(jsonPath("$.data.place.openStatus").value("OPEN"))
                .andExpect(jsonPath("$.data.place.reviewSummary.averageRating").value(4.8))
                .andExpect(jsonPath("$.data.place.reviewSummary.totalCount").value(128))
                .andExpect(jsonPath("$.data.place.reviewSummary.reviews.length()").value(2))
                .andExpect(jsonPath("$.data.place.reviewSummary.reviews[0].author.nickname").value("현우님"))
                .andExpect(jsonPath("$.data.place.reviewSummary.reviews[1].author.profileImageUrl").doesNotExist())
                .andExpect(jsonPath("$.data.place.viewer.hasSaved").value(false))
                .andExpect(jsonPath("$.data.nearbyCourses.length()").value(2))
                .andExpect(jsonPath("$.data.nearbyCourses[0].title").value("비 오는 날 성수 감성 카페 코스"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
        }

        @Test
        fun `없는 장소면 4043 에러`() {
            mockMvc
                .perform(get("/service/v1/places/999999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4043))
                .andExpect(jsonPath("$.message").value("장소를 찾을 수 없습니다: id=999999"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        fun `mockError로 모킹 에러를 주입한다`() {
            mockMvc
                .perform(get("/service/v1/places/101?mockError=4040"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
                .andExpect(jsonPath("$.data").doesNotExist())
        }
    }
