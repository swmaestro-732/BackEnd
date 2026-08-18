package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.SavedPlacesCommand
import com.example.backend.user.application.port.inbound.dto.SavedPlacesResult
import com.example.backend.user.domain.model.SavedPlace

/**
 * 인바운드 포트 — 장소 저장(saved_places)·저장 장소 조회(공개 API).
 *
 * 저장 레코드는 user 도메인 소유 데이터다. 저장 시 장소 존재를 place 인바운드 포트로 검증하고(그 외 404),
 * 이미 저장한 장소면 중복 저장으로 막는다(409) — 장소당 저장 레코드는 1개다.
 * 저장 시점의 장소 카테고리를 스냅샷([SavedPlace.category])으로 복사해 둔다(MSA 대비 비정규화).
 * 저장 취소(unsave)는 저장과 대칭인 멱등 연산이다 — 저장돼 있지 않아도 성공으로 수렴한다(코스 저장 취소 선례와 동일).
 * 조회는 저장 레코드(ID 위주)만 반환한다.
 */
interface SavedPlaceUseCase {
    fun save(
        userId: Long,
        placeId: Long,
    ): SavedPlace

    /** (userId, placeId) 저장을 취소한다. 저장 레코드가 없어도 오류 없이 성공한다(멱등). */
    fun unsave(
        userId: Long,
        placeId: Long,
    )

    fun getSavedPlaces(command: SavedPlacesCommand): SavedPlacesResult
}
