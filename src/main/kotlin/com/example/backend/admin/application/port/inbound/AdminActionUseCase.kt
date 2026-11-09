package com.example.backend.admin.application.port.inbound

/**
 * 어드민 조치 감사 기록 인바운드 포트(admin_actions). 다른 도메인의 어드민 기능(코스, 이후 장소와 신고)이
 * 자기 어댑터에서 이 포트로 조치를 남긴다. 호출한 쪽 트랜잭션에 참여하므로 조치가 롤백되면 기록도 남지 않는다.
 */
interface AdminActionUseCase {
    fun record(
        adminEmail: String,
        targetType: String,
        targetId: Long,
        action: String,
        memo: String? = null,
    )
}
