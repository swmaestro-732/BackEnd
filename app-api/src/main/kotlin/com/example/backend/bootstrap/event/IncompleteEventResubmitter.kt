package com.example.backend.bootstrap.event

import org.springframework.modulith.events.IncompleteEventPublications
import org.springframework.modulith.events.ResubmissionOptions
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 이벤트 저장소(event_publication)의 FAILED 기록을 주기적으로 재발행한다.
 * 리스너가 예외로 끝난 기록(예: 동기 반영과 SQS 폴백이 모두 실패)은 바로 FAILED 가 되고, 처리 중 서버가 내려가
 * PUBLISHED, PROCESSING, RESUBMITTED 에 멈춘 기록은 spring.modulith.events.staleness.* 가 FAILED 로 바꿔 여기로 온다.
 * 기동 시 재발행(republish-outstanding-events-on-restart)은 끈다. 롤링 배포에서 인스턴스마다 다시 보내기 때문이다.
 * 완료된 기록은 completion-mode: delete 로 바로 지워지므로 별도 정리 잡이 없다.
 *
 * ponytail: 여러 인스턴스가 같은 FAILED 기록을 동시에 재발행할 수 있다. 소비자가 멱등이라 허용한다
 * (코스 개수는 eventId 로 processed_course_count_events 에서 중복 제거, 저장 장소 색인은 DB 를 다시 읽어 upsert/delete).
 * 중복 실행이 문제가 되면 ShedLock 으로 한 인스턴스만 돌린다.
 * 기록은 리스너 메서드 이름(listener_id)과 이벤트 클래스 이름(event_type)으로 다시 찾는다. 리스너나 이벤트 이름을 바꾸면
 * 남은 기록은 대상을 못 찾아 FAILED 로 남으니, 바꾸기 전에 해당 미완료 기록을 비우고(처리 완료 확인 후 삭제) 배포한다.
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
        incompleteEventPublications.resubmitIncompletePublications(OPTIONS)
    }

    private companion object {
        /**
         * FAILED 기록만 대상이다. 발행 후 MIN_AGE 가 지난 것만 골라 막 실패한 기록을 곧바로 되풀이하지 않고,
         * 한 번에 BATCH_SIZE 건, 재발행 중(RESUBMITTED)인 기록이 MAX_IN_FLIGHT 건 이상이면 이번 회차를 건너뛴다.
         */
        val OPTIONS: ResubmissionOptions =
            ResubmissionOptions
                .defaults()
                .withMinAge(Duration.ofMinutes(5))
                .withBatchSize(100)
                .withMaxInFlight(100)
    }
}
