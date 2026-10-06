package com.example.backend.area.domain.model

/**
 * 법정동코드 값 타입. 10자리 숫자 문자열이며, 앞자리 prefix 로 상위 행정구역을 파생한다.
 *   전체 10자리 = 읍/면/동, 앞 5자리 = 시/군/구, 앞 2자리 = 시/도
 *   예) 1168010100 → 시도 11(서울) / 시군구 11680(강남구) / 읍면동 역삼동
 *
 * 생성 시점에 형식을 검증하므로, 유효한 [AreaCode] 인스턴스는 항상 10자리 숫자임이 보장된다.
 */
@JvmInline
value class AreaCode(
    val value: String,
) {
    init {
        require(value.matches(CODE_PATTERN)) { "법정동코드는 10자리 숫자여야 합니다: $value" }
    }

    /** 시/도 코드(앞 2자리). */
    val sidoCode: String get() = value.take(2)

    /** 시/군/구 코드(앞 5자리). */
    val sigunguCode: String get() = value.take(5)

    companion object {
        private val CODE_PATTERN = Regex("^[0-9]{10}$")
    }
}
