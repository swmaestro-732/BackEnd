package com.example.backend.architecture

import com.example.backend.BackendApplication
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

/**
 * Spring Modulith 로 도메인 패키지(모듈) 경계를 검증한다. 예외 없이 verify() 를 통과해야 한다.
 * 모듈 = com.example.backend 바로 아래 패키지(user, course, place ...). 다른 모듈은 공개한 것만 쓸 수 있고, 모듈 사이 순환은 없어야 한다.
 * - 도메인: application.port.inbound(와 dto)를 공개. user 는 이벤트(application.event)와 SPI(application.port.spi)도 공개
 * - common: 공유 커널이라 전부 공개(OPEN). web(core-web): 예외 핸들러 등 하위 패키지도 app-api 가 써서 OPEN
 * - 내부 계층 클라이언트(opensearch, s3, sqs, kakao, tmap, google, naver, fcm): 공개 타입이 모듈 최상위 패키지에 있어 그대로 공개
 * 모듈 안 레이어 규칙(domain 이 adapter 를 모름 등)은 HexagonalArchitectureTest 가 맡는다.
 */
class ModulithVerificationTest {
    @Test
    fun `모듈 경계를 지키고 모듈 사이 순환이 없다`() {
        ApplicationModules.of(BackendApplication::class.java).verify()
    }
}
