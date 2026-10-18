package com.example.backend.report.application.port.outbound

import com.example.backend.report.domain.model.ReportTargetType

/**
 * 아웃바운드 포트 — 신고 대상의 존재와 소유자를 확인한다.
 * report 코어는 코스·사용자·댓글 도메인을 직접 알지 않고 ACL 어댑터가 각 도메인의 인바운드 포트에 위임한다.
 */
interface ReportTargetLookupPort {
    /** 대상의 소유자(작성자) id. 대상이 없거나 [viewerId] 가 볼 수 없으면 해당 도메인의 NOT_FOUND 예외를 던진다. */
    fun getOwnerId(
        type: ReportTargetType,
        targetId: Long,
        viewerId: Long,
    ): Long
}
