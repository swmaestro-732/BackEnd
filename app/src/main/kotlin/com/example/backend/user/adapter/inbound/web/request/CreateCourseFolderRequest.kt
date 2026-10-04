package com.example.backend.user.adapter.inbound.web.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 코스 저장 폴더 생성 요청 — 웹 어댑터 DTO.
 * 노션 필드 명세 미작성 상태라 디자인(저장 → 새 폴더 만들기 시트)에서 도출한 필드.
 *
 * - name: 폴더 이름. 최대 10자(가운데 공백 포함). 같은 사용자 안에서 중복 불가(409).
 *
 * **앞뒤 공백은 서버가 잘라내고, 길이·공백 검증도 잘라낸 값 기준이다.**
 * 트림은 이름 유일성 정규화용이다 — `(user_id, name)` 유니크 인덱스는 `"데이트"` 와 `"데이트 "`(뒤 공백)를
 * 다른 값으로 보므로, 트림하지 않으면 눈으로 구분되지 않는 폴더가 둘 생긴다.
 * **가운데 띄어쓰기는 막지 않는다**(`"데이트 코스"` 는 그대로 저장된다).
 *
 * 검증을 원본이 아니라 [name](트림된 값) 게터에 걸어 둔 이유: `@Size` 가 원본에 걸리면
 * 앞뒤 공백까지 길이에 세어, 화면 글자수 카운터(트림 기준)와 서버 판정이 경계값에서 어긋난다.
 * 검증 대상 프로퍼티 이름이 `name` 이라 실패 응답의 `fieldErrors[].field` 도 `name` 으로 나간다.
 *
 * 디자인의 설명·비공개 토글은 이번 실구현 범위에서 제외했다(스키마는 name·order_no 만 유지).
 */
data class CreateCourseFolderRequest(
    @param:JsonProperty("name") private val rawName: String,
) {
    @get:NotBlank
    @get:Size(max = 10)
    val name: String get() = rawName.trim()
}
