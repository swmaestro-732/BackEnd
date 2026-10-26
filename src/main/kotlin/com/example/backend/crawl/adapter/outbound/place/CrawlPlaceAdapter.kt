package com.example.backend.crawl.adapter.outbound.place

import com.example.backend.common.geo.Coordinate
import com.example.backend.crawl.application.port.inbound.dto.CrawledPlaceCommand
import com.example.backend.crawl.application.port.outbound.CrawlPlacePort
import com.example.backend.crawl.application.port.outbound.ResolvedCrawlPlace
import com.example.backend.place.application.port.inbound.PlaceRegistrationUseCase
import com.example.backend.place.application.port.inbound.dto.RegisterPlaceCommand
import org.springframework.stereotype.Component

@Component
class CrawlPlaceAdapter(
    private val registration: PlaceRegistrationUseCase,
) : CrawlPlacePort {
    override fun resolve(places: List<CrawledPlaceCommand>): List<ResolvedCrawlPlace> =
        registration
            .register(
                places.map { place ->
                    RegisterPlaceCommand(
                        kakaoPlaceId = place.kakaoPlaceId,
                        name = place.name,
                        category = place.category,
                        coordinate = Coordinate(place.lat, place.lng),
                        address = place.address,
                        description = place.description,
                        imageUrl = place.imageUrls.firstOrNull { it.isNotBlank() },
                    )
                },
            ).map { ResolvedCrawlPlace(it.kakaoPlaceId, it.id, it.coordinate) }
}
