package com.example.backend.mobile.home.application.service

import com.example.backend.mobile.home.application.port.inbound.HomeFeedUseCase
import com.example.backend.mobile.home.application.port.inbound.dto.HomeFeedResult
import com.example.backend.mobile.home.application.port.outbound.HomeFeedPort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCursor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 피드 화면 조합 서비스 (BFF). 자신의 아웃바운드 포트([HomeFeedPort])만 호출해 한 화면 응답 재료를 만든다.
 * 타 도메인 인바운드에 직접 의존하지 않아 MSA 분리 시 어댑터만 교체하면 된다.
 *
 * 저장수 랭킹은 course 도메인이 소유한 denormalized `courses.saves_cnt`(저장/취소 시 갱신)를 기준으로
 * SQL(saves_cnt DESC, created_at DESC, id DESC)에서 정렬·키셋 조회되어 내려오며,
 * BFF 는 외부 문자열 커서의 디코딩과 다음 커서 인코딩을 담당한다.
 */
@Service
@Transactional(readOnly = true)
class HomeFeedService(
    private val homeFeedPort: HomeFeedPort,
) : HomeFeedUseCase {
    override fun getFeed(
        cursor: String?,
        size: Int,
    ): HomeFeedResult {
        val page = homeFeedPort.listPublicCandidates(HomeFeedCursorCodec.decode(cursor), size)
        val nextCursor =
            if (page.hasNext) {
                page.courses.last().let {
                    HomeFeedCursorCodec.encode(
                        HomeFeedCursor(
                            savesCnt = it.savesCnt,
                            createdAt = it.createdAt,
                            id = it.id,
                        ),
                    )
                }
            } else {
                null
            }
        return HomeFeedResult(
            courses = page.courses,
            nextCursor = nextCursor,
            hasNext = page.hasNext,
        )
    }
}
