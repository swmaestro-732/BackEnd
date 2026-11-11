package com.example.backend.mobile.aichat.adapter.inbound.web.response

import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatQuickReply
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatStep
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatTurnResult
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatCourse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** [AiChatTurnResponse] — 턴 결과를 앱 응답(step 이름·말풍선·칩·입력창·코스 카드)으로 옮기는 매핑을 검증한다. */
class AiChatTurnResponseTest {
    @Test
    fun `결과 턴은 칩·입력창·코스 카드를 홈 카드와 같은 모양으로 옮긴다`() {
        val result =
            AiChatTurnResult(
                sessionId = "s1",
                step = AiChatStep.RESULT,
                messages = listOf("이런 코스는 어때요?"),
                quickReplies = listOf(AiChatQuickReply("더 추천받기", "RETRY"), AiChatQuickReply("내 근처", "NEAR_ME", true)),
                inputEnabled = false,
                inputPlaceholder = null,
                courses =
                    listOf(
                        AiChatCourse(1L, "코스 1", "https://img/1.jpg", listOf("DATE", "CAFETOUR"), "성수", 3, 20, 1, 2),
                        AiChatCourse(2L, "코스 2", null, emptyList(), null, 0, 0, 0, 0),
                    ),
            )

        val response = AiChatTurnResponse.from(result)

        assertThat(response.sessionId).isEqualTo("s1")
        assertThat(response.step).isEqualTo("RESULT")
        assertThat(response.messages).containsExactly(AiChatTurnResponse.Message("이런 코스는 어때요?"))
        assertThat(response.quickReplies).containsExactly(
            AiChatTurnResponse.QuickReply("더 추천받기", "RETRY", false),
            AiChatTurnResponse.QuickReply("내 근처", "NEAR_ME", true),
        )
        assertThat(response.input).isEqualTo(AiChatTurnResponse.Input(enabled = false, placeholder = null))
        assertThat(response.courses).containsExactly(
            AiChatTurnResponse.Course(
                1L,
                "코스 1",
                "https://img/1.jpg",
                "DATE",
                listOf("DATE", "CAFETOUR"),
                "성수",
                3,
                20,
                1,
                2,
            ),
            AiChatTurnResponse.Course(2L, "코스 2", null, null, emptyList(), null, 0, 0, 0, 0),
        )
    }

    @Test
    fun `질문 턴은 코스 없이 입력창 상태만 옮긴다`() {
        val response =
            AiChatTurnResponse.from(
                AiChatTurnResult("s1", AiChatStep.THEME, listOf("테마?"), emptyList(), true, "분위기를 적어도 돼요", emptyList()),
            )

        assertThat(response.step).isEqualTo("THEME")
        assertThat(response.input).isEqualTo(AiChatTurnResponse.Input(enabled = true, placeholder = "분위기를 적어도 돼요"))
        assertThat(response.courses).isEmpty()
        assertThat(response.quickReplies).isEmpty()
    }
}
