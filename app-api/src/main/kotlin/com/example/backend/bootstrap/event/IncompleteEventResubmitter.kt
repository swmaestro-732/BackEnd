package com.example.backend.bootstrap.event

import org.springframework.modulith.events.IncompleteEventPublications
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 이벤트 저장소(event_publication)에 남은 미완료 기록을 주기적으로 재발행한다.
 * 리스너가 실패했거나(예: 동기 반영과 SQS 폴백이 모두 실패) 처리 중 서버가 내려간 경우다. 기동 시 재발행은
 * spring.modulith.events.republish-outstanding-events-on-restart 가 맡고, 이 잡은 실행 중인 서버의 재시도를 맡는다.
 * 완료된 기록은 completion-mode: delete 로 바로 지워지므로 별도 정리 잡이 없다.
 *
 * ponytail: 여러 인스턴스가 같은 기록을 동시에 재발행할 수 있다. 소비자가 멱등이라 허용한다
 * (코스 개수는 eventId 로 processed_course_count_events 에서 중복 제거, 색인은 upsert/delete).
 * 중복 실행이 문제가 되면 ShedLock 으로 한 인스턴스만 돌린다.
 */
@Component
class IncompleteEventResubmitter(
    private val incompleteEventPublications: IncompleteEventPublications,
) {
    @Scheduled(
        fixedDelayString = "\${event.resubmission.interval:PT5M}",
        initialDelayString = "\${event.resubmission.interval:PT5M}",
    )
    fun resubmit() {
        incompleteEventPublications.resubmitIncompletePublicationsOlderThan(MIN_AGE)
    }

    private companion object {
        /** 막 발행돼 아직 처리 중인 기록을 건드리지 않도록 이만큼 지난 것만 재발행한다. */
        val MIN_AGE: Duration = Duration.ofMinutes(5)
    }
}
