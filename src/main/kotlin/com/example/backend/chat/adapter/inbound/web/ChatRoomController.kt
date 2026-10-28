package com.example.backend.chat.adapter.inbound.web

import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.chat.adapter.inbound.web.request.JoinChatRoomRequest
import com.example.backend.chat.adapter.inbound.web.response.ChatMemberResponse
import com.example.backend.chat.adapter.inbound.web.response.ChatRoomsAtResponse
import com.example.backend.chat.adapter.inbound.web.response.MyChatRoomListResponse
import com.example.backend.chat.application.port.inbound.ChatRoomUseCase
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ChatRoomController(
    private val chatRoomUseCase: ChatRoomUseCase,
) {
    @GetMapping("/api/v1/chat-rooms")
    @AccessTokenRequired
    fun findRoomsAt(
        @RequestParam lat: Double,
        @RequestParam lng: Double,
    ): ApiResponse<ChatRoomsAtResponse> =
        ApiResponse.success(ChatRoomsAtResponse.from(chatRoomUseCase.findRoomsAt(Coordinate(lat, lng))))

    @PostMapping("/api/v1/chat-rooms/{roomId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @AccessTokenRequired
    fun join(
        @PathVariable roomId: Long,
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: JoinChatRoomRequest,
    ): ApiResponse<ChatMemberResponse> =
        ApiResponse.success(ChatMemberResponse.from(chatRoomUseCase.join(request.toCommand(userId, roomId))))

    @DeleteMapping("/api/v1/chat-rooms/{roomId}/members/me")
    @AccessTokenRequired
    fun leave(
        @PathVariable roomId: Long,
        @CurrentUserId userId: Long,
    ): ApiResponse<Nothing?> {
        chatRoomUseCase.leave(userId, roomId)
        return ApiResponse.ok()
    }

    @GetMapping("/api/v1/my/chat-rooms")
    fun findMyRooms(
        @CurrentUserId userId: Long,
    ): ApiResponse<MyChatRoomListResponse> =
        ApiResponse.success(MyChatRoomListResponse.from(chatRoomUseCase.findMyRooms(userId)))
}
