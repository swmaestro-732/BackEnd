package com.example.backend.media.application.port.inbound

/**
 * 미디어(S3) 정리 유스케이스 — 참조가 끊긴 고아 이미지를 삭제한다.
 *
 * 프로필 이미지 교체, 회원 탈퇴 등 이미지 참조가 바뀌는 여러 지점에서 재사용한다
 * (크로스 도메인에서 이 인바운드 포트로만 호출).
 */
interface MediaCleanupUseCase {
    /**
     * CDN 이미지 URL 이 우리 스토리지(cdnBaseUrl)의 것이면 해당 객체를 삭제한다.
     * null·빈 값·외부 URL 은 무시하며, 삭제 실패는 흐름을 막지 않는다(fail-soft).
     */
    fun deleteByUrl(imageUrl: String?)
}
