package com.example.backend.area.domain.model

/**
 * 행정구역(법정동) 기준정보 — area 테이블 한 행의 타입 있는 표현이다.
 *
 * 애그리거트가 아니다: 불변식·상태 전이·행위가 없고, 데이터 수명은 시드(Flyway)가 소유하며 앱은 읽기만 한다.
 * 그래서 팩토리·private 생성자 같은 보호 장치 없이 평범한 data class 로 둔다.
 * 형식 불변식(10자리 코드)은 [AreaCode] 가 소유하므로, 이 타입의 인스턴스는 항상 유효한 코드를 갖는다.
 *
 * Exposed·Spring 등 프레임워크 타입에는 의존하지 않는다(ArchUnit 이 강제).
 */
data class Area(
    val code: AreaCode,
    val sidoName: String,
    val sigunguName: String?,
    val dongName: String,
    val isActive: Boolean,
)
