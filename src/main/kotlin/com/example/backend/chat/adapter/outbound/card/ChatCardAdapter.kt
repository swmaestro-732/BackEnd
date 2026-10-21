package com.example.backend.chat.adapter.outbound.card

import com.example.backend.chat.application.port.outbound.ChatCardPort
import com.example.backend.chat.domain.model.ChatCard
import com.example.backend.chat.domain.model.ChatCardType
import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import org.springframework.stereotype.Component

/** 오픈채팅이라 누구나 보므로 코스는 전체 공개·발행된 것만 공유한다. */
@Component
class ChatCardAdapter(
    private val placeQueryUseCase: PlaceQueryUseCase,
    private val courseQueryUseCase: CourseQueryUseCase,
) : ChatCardPort {
    override fun findCard(
        type: ChatCardType,
        refId: Long,
        viewerId: Long,
    ): ChatCard? =
        when (type) {
            ChatCardType.PLACE -> {
                placeQueryUseCase.findPlacesById(listOf(refId)).firstOrNull()?.let {
                    ChatCard(type, it.id, it.name, it.address, it.imageUrl?.ifBlank { null })
                }
            }

            ChatCardType.COURSE -> {
                courseQueryUseCase
                    .getDetails(listOf(refId), viewerId)
                    .firstOrNull()
                    ?.takeIf { it.visibility == CourseVisibility.PUBLIC && it.isPublished }
                    ?.let {
                        val subtitle = listOfNotNull(it.area, "장소 ${it.places.size}곳").joinToString(" · ")
                        ChatCard(type, it.id, it.title, subtitle, it.coverImageUrl.ifBlank { null })
                    }
            }
        }
}
