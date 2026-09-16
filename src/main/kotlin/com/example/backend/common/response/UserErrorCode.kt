package com.example.backend.common.response

enum class UserErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    USER_NOT_FOUND(404, 4042, "사용자를 찾을 수 없습니다."),
    NICKNAME_ALREADY_TAKEN(409, 4091, "이미 사용 중인 닉네임입니다."),
    HANDLE_ALREADY_TAKEN(409, 4092, "이미 사용 중인 핸들입니다."),
    COURSE_ALREADY_SAVED(409, 4093, "이미 저장한 코스입니다."),

    // 4094 는 장소 저장 중복(PlaceErrorCode.PLACE_ALREADY_SAVED, SCRUM-427)이 선점 — 번호가 겹치지 않게 4095 로 채번했다.
    FOLDER_NAME_ALREADY_TAKEN(409, 4095, "이미 사용 중인 폴더 이름입니다."),
}
