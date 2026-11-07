package com.example.backend.user.domain.model

/**
 * 코스 저장 폴더 — 사용자가 저장 코스를 분류하는 폴더(saved_course_folders).
 *
 * 폴더는 사용자 소유 데이터라 userId 로만 묶이고, [orderNo] 는 폴더 칩 노출 순서다(작을수록 앞 — 새 폴더가 맨 뒤).
 * 이름은 같은 사용자 안에서 유일하다.
 */
data class CourseFolder(
    val id: Long,
    val userId: Long,
    val name: String,
    val orderNo: Short,
)
