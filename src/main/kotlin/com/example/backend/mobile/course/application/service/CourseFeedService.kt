package com.example.backend.mobile.course.application.service

import com.example.backend.mobile.course.application.port.inbound.CourseFeedUseCase
import com.example.backend.mobile.course.application.port.inbound.dto.CourseFeedResult
import com.example.backend.mobile.course.application.port.outbound.FeedCoursePort
import com.example.backend.mobile.course.application.port.outbound.FeedSavesPort
import com.example.backend.mobile.course.application.port.outbound.dto.FeedCourse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 피드 화면 조합 서비스 (BFF). 자신의 아웃바운드 포트만 호출해 한 화면 응답 재료를 만든다 —
 * 공개 코스 후보([FeedCoursePort]) + 코스별 저장수([FeedSavesPort]). 타 도메인 인바운드에 직접 의존하지 않아
 * MSA 분리 시 어댑터만 교체하면 된다. 조합 한 번을 한 읽기 트랜잭션으로 묶는다.
 *
 * 저장수 랭킹은 최신 후보 상한([CANDIDATE_CAP])만 메모리에서 정렬한다(MVP). 데이터가 커지면 ORDER BY saves 를 SQL 로 내린다.
 */
@Service
@Transactional(readOnly = true)
class CourseFeedService(
    private val feedCoursePort: FeedCoursePort,
    private val feedSavesPort: FeedSavesPort,
) : CourseFeedUseCase {
    override fun getFeed(limit: Int): CourseFeedResult {
        val candidates = feedCoursePort.listPublicCandidates(CANDIDATE_CAP)
        val saves = feedSavesPort.countSaves(candidates.map { it.id })
        val ranked =
            candidates
                .map { it.copy(savesCnt = saves[it.id] ?: 0) }
                .sortedWith(
                    compareByDescending<FeedCourse> { it.savesCnt }.thenByDescending { it.createdAt },
                ).take(limit)
        return CourseFeedResult(ranked)
    }

    private companion object {
        // ponytail: 최신 후보만 상한을 두고 메모리 정렬(MVP). 데이터가 커지면 ORDER BY saves 를 SQL 로 내린다.
        const val CANDIDATE_CAP = 200
    }
}
