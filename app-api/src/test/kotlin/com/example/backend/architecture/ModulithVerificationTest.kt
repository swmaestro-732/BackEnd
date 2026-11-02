package com.example.backend.architecture

import com.example.backend.BackendApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

/**
 * Spring Modulith 로 도메인 패키지(모듈) 경계를 검증한다.
 * 모듈 = com.example.backend 바로 아래 패키지(user, course, place ...). 다른 모듈은 공개한 것만 쓸 수 있다.
 * - 도메인: application.port.inbound(와 dto)만 @NamedInterface 로 공개
 * - common: 공유 커널이라 전부 공개(OPEN). web, opensearch: 타입이 모듈 최상위에 있어 그대로 공개
 * 모듈 안 레이어 규칙(domain 이 adapter 를 모름 등)은 HexagonalArchitectureTest 가 맡는다.
 */
class ModulithVerificationTest {
    private val modules = ApplicationModules.of(BackendApplication::class.java)

    @Test
    fun `알려진 예외(course, user 순환) 말고는 모듈 위반이 없다`() {
        val violations = modules.detectViolations().messages

        assertThat(violations.filterNot(::isKnownCourseUserCycle)).isEmpty()
        // 순환을 끊으면 이 단언이 실패한다. 그때 예외 처리를 지우고 modules.verify() 로 바꾼다.
        assertThat(violations.filter(::isKnownCourseUserCycle)).isNotEmpty()
    }

    /** user 가 course 를 부르고(ACL), course 도 user 를 부른다(보는 사람 상태, 댓글 작성자). 이벤트 전환으로 끊을 예정. */
    private fun isKnownCourseUserCycle(message: String): Boolean =
        message.startsWith("Cycle detected") &&
            Regex("""Slice (\w+)""").findAll(message).map { it.groupValues[1] }.toSet() == setOf("course", "user")
}
