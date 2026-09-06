package com.example.backend.common.response

enum class PlaceErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    PLACE_NOT_FOUND(404, 4043, "장소를 찾을 수 없습니다."),

    // 4094 는 장소 저장 중복이 선점 — 폴더 이름 중복(UserErrorCode.FOLDER_NAME_ALREADY_TAKEN)은 4095 로 채번했다.
    PLACE_ALREADY_SAVED(409, 4094, "이미 저장한 장소입니다."),
}
