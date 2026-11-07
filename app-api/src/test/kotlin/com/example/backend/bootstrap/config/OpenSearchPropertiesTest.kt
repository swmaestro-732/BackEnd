package com.example.backend.bootstrap.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpenSearchPropertiesTest {
    @Test
    fun `prefix 가 빈값이면 이름을 그대로 둔다(현행 prod 동일)`() {
        val props = OpenSearchProperties()

        assertThat(props.withPrefix("place")).isEqualTo("place")
        assertThat(props.withPrefix("place_v1")).isEqualTo("place_v1")
        assertThat(props.withPrefix("course")).isEqualTo("course")
        assertThat(props.withPrefix("course_v1")).isEqualTo("course_v1")
    }

    @Test
    fun `prefix 가 있으면 alias·물리 인덱스 이름 앞에 붙인다`() {
        val props = OpenSearchProperties(indexPrefix = "dev-")

        assertThat(props.withPrefix("place")).isEqualTo("dev-place")
        assertThat(props.withPrefix("place_v1")).isEqualTo("dev-place_v1")
        assertThat(props.withPrefix("course")).isEqualTo("dev-course")
        assertThat(props.withPrefix("course_v1")).isEqualTo("dev-course_v1")
    }
}
