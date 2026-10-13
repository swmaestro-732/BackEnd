plugins {
    id("backend.domain-conventions")
}

description = "report 컨텍스트 — 코스·사용자·댓글 신고"

dependencies {
    implementation(project(":user-api")) // 신고 대상 사용자 존재 확인
    implementation(project(":course")) // 신고 대상 코스·댓글 존재 확인
}
