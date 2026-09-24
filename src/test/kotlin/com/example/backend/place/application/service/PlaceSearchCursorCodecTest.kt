package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.Base64

class PlaceSearchCursorCodecTest {
    @Test
    fun `오프셋·재검색 여부·기준 장소를 왕복한다`() {
        assertThat(PlaceSearchCursorCodec.decode(PlaceSearchCursorCodec.encode(20, false)))
            .isEqualTo(PlaceSearchCursor(20, false, null))
        assertThat(PlaceSearchCursorCodec.decode(PlaceSearchCursorCodec.encode(30, true, 99L)))
            .isEqualTo(PlaceSearchCursor(30, true, 99L))
        assertThat(PlaceSearchCursorCodec.decode(null)).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["os", "os:0", "os:10:0", "os:10:1:2", "db:5", "near:1:2", "osf:x"])
    fun `형식이 어긋난 커서는 400 이다`(payload: String) {
        val cursor = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        assertThatThrownBy { PlaceSearchCursorCodec.decode(cursor) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `Base64 가 아닌 커서도 400 이다`() {
        assertThatThrownBy { PlaceSearchCursorCodec.decode("%%%") }
            .isInstanceOf(BusinessException::class.java)
    }
}
