// :user-api - user 도메인의 공개 계약(inbound 포트 + DTO)만 담는다. 다른 도메인(course)과 BFF(mobile)가
// user 구현체가 아닌 이 계약에만 의존해 크로스도메인 순환을 끊는다. 컨텍스트 api/impl 분할의 첫 사례.
// 순수 인터페이스/데이터 클래스만 - 인프라 의존 없음(리프).
dependencies {
}
