package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.place.application.port.inbound.PlaceRegistrationUseCase
import com.example.backend.place.application.port.inbound.dto.RegisterPlaceCommand
import com.example.backend.place.application.port.inbound.dto.RegisteredPlaceResult
import com.example.backend.place.application.port.outbound.AreaCodeLookupPort
import com.example.backend.place.application.port.outbound.PlacePersistencePort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceCategory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionOperations

/** 검색·크롤링 공통 등록 경로. 외부 지역 조회는 DB 커넥션을 점유하지 않는다. */
@Service
class PlaceRegistrationService(
    private val placePersistencePort: PlacePersistencePort,
    private val areaCodeLookupPort: AreaCodeLookupPort,
    private val transactionOperations: TransactionOperations,
    private val eventPublisher: ApplicationEventPublisher,
) : PlaceRegistrationUseCase {
    override fun register(commands: List<RegisterPlaceCommand>): List<RegisteredPlaceResult> {
        if (commands.isEmpty()) return emptyList()
        commands.forEach { command ->
            if (PlaceCategory.entries.none { it.name == command.category }) {
                throw BusinessException(CommonErrorCode.INVALID_INPUT, "unknown category: ${command.category}")
            }
        }
        val allKakaoIds = commands.map { it.kakaoPlaceId }
        val existingKakaoIds =
            transactionOperations
                .execute {
                    placePersistencePort.findByKakaoIds(allKakaoIds).mapNotNull { it.kakaoPlaceId }.toSet()
                }.orEmpty()
        val toInsert =
            commands
                .asSequence()
                .filter { it.kakaoPlaceId.isNotBlank() && it.kakaoPlaceId !in existingKakaoIds }
                .distinctBy { it.kakaoPlaceId }
                .filter { it.name.isNotBlank() && it.address.isNotBlank() }
                .map { command ->
                    Place.create(
                        name = command.name,
                        description = command.description?.takeIf { it.isNotBlank() },
                        category = PlaceCategory.valueOf(command.category),
                        location = command.coordinate,
                        address = command.address,
                        areaCode = areaCodeLookupPort.findLegalDongCode(command.coordinate),
                        imageUrl = command.imageUrl?.takeIf { it.isNotBlank() },
                        kakaoPlaceId = command.kakaoPlaceId,
                    )
                }.toList()
        return transactionOperations
            .execute {
                if (toInsert.isNotEmpty()) placePersistencePort.insertIgnoringConflicts(toInsert)
                val saved = placePersistencePort.findByKakaoIds(allKakaoIds)
                // 재조회는 동시 insert ignore로 다른 요청이 저장한 행도 포함한다.
                eventPublisher.publishEvent(PlacesSavedEvent(saved))
                saved.map { place ->
                    RegisteredPlaceResult(
                        place.id!!,
                        place.kakaoPlaceId!!,
                        place.category.name,
                        place.location,
                        place.imageUrl,
                    )
                }
            }.orEmpty()
    }
}
