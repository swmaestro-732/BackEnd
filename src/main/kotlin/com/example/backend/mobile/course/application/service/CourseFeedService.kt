package com.example.backend.mobile.course.application.service

import com.example.backend.mobile.course.application.port.inbound.CourseFeedUseCase
import com.example.backend.mobile.course.application.port.inbound.dto.CourseFeedResult
import com.example.backend.mobile.course.application.port.outbound.FeedCoursePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 피드 화면 조합 서비스 (BFF). 자신의 아웃바운드 포트([FeedCoursePort])만 호출해 한 화면 응답 재료를 만든다.
 * 타 도메인 인바운드에 직접 의존하지 않아 MSA 분리 시 어댑터만 교체하면 된다.
 *
 * 저장수 랭킹은 course 도메인이 소유한 denormalized `courses.saves_cnt`(저장/취소 시 갱신)를 기준으로
 * SQL(saves_cnt DESC, created_at DESC)에서 정렬·limit 되어 내려오므로, BFF 는 그대로 전달만 한다.
 */
@Service
@Transactional(readOnly = true)
class CourseFeedService(
    private val feedCoursePort: FeedCoursePort,
) : CourseFeedUseCase {
    override fun getFeed(limit: Int): CourseFeedResult = CourseFeedResult(feedCoursePort.listPublicCandidates(limit))
}
