plugins {
    id("backend.domain-conventions")
}

description = "mobile BFF — 화면 단위 조합 API(/service/v1/**). 각 컨텍스트의 인바운드 포트만 호출한다."

dependencies {
    implementation(project(":user-api"))
    implementation(project(":course"))
    implementation(project(":place"))
    implementation(project(":area"))
}
