package com.example.backend.user.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.outbound.UserLastLocationPersistencePort
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class UserLastLocationServiceTest {
    private val userLastLocationPersistencePort = mock(UserLastLocationPersistencePort::class.java)
    private val service = UserLastLocationService(userLastLocationPersistencePort)

    @Test
    fun `최근 위치를 소수 3자리로 반올림해 저장을 맡긴다`() {
        service.record(1L, Coordinate(37.5445678, 127.0561234))

        verify(userLastLocationPersistencePort).saveIfActive(1L, Coordinate(37.545, 127.056))
    }
}
