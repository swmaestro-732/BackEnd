package com.example.backend.mobile.aichat.adapter.inbound.web.response

import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatTurnResult
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatCourse

/**
 * 웹 응답 DTO — 커미 AI 의 한 턴. 앱은 [messages] 를 말풍선으로, [quickReplies] 를 칩으로 그리고 [input] 으로 입력창을 켜고 끈다.
 * [step] 이 REGION 이면 quickReplies 는 비어 있고 앱이 자기 지역 칩을 다시 보여준다.
 * [courses] 는 RESULT 에서만 채워지며 필드는 홈 추천 코스 카드(`HomeResponse.Course`)와 같다.
 */
data class AiChatTurnResponse(
    val sessionId: String,
    val step: String,
    val messages: List<Message>,
    val quickReplies: List<QuickReply>,
    val input: Input,
    val courses: List<Course>,
) {
    data class Message(
        val text: String,
    )

    /** 칩 하나. value 는 앱이 해석하지 않고 되돌려 보내며, `RESTART` 만 앱이 첫 화면 복귀로 처리한다. */
    data class QuickReply(
        val label: String,
        val value: String,
        val requiresLocation: Boolean,
    )

    data class Input(
        val enabled: Boolean,
        val placeholder: String?,
    )

    data class Course(
        val id: Long,
        val title: String,
        val coverImageUrl: String?,
        val theme: String?,
        val themes: List<String>,
        val area: String?,
        val placeCount: Int,
        val walkingMinutes: Int,
        val likesCnt: Int,
        val savesCnt: Int,
    ) {
        companion object {
            fun from(course: AiChatCourse) =
                Course(
                    id = course.id,
                    title = course.title,
                    coverImageUrl = course.coverImageUrl,
                    theme = course.themes.firstOrNull(),
                    themes = course.themes,
                    area = course.area,
                    placeCount = course.placeCount,
                    walkingMinutes = course.walkingMinutes,
                    likesCnt = course.likesCnt,
                    savesCnt = course.savesCnt,
                )
        }
    }

    companion object {
        fun from(result: AiChatTurnResult): AiChatTurnResponse =
            AiChatTurnResponse(
                sessionId = result.sessionId,
                step = result.step.name,
                messages = result.messages.map(::Message),
                quickReplies = result.quickReplies.map { QuickReply(it.label, it.value, it.requiresLocation) },
                input = Input(enabled = result.inputEnabled, placeholder = result.inputPlaceholder),
                courses = result.courses.map(Course::from),
            )
    }
}
