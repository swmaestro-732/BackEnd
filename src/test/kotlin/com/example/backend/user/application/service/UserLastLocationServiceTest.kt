package com.example.backend.user.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.outbound.UserLastLocationPersistencePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class UserLastLocationServiceTest {
    private val userLastLocationPersistencePort = mock(UserLastLocationPersistencePort::class.java)
    private val userPersistencePort = mock(UserPersistencePort::class.java)
    private val service = UserLastLocationService(userLastLocationPersistencePort, userPersistencePort)

    @Test
    fun `활성 사용자면 최근 위치를 저장한다`() {
        `when`(userPersistencePort.lockActive(listOf(1L))).thenReturn(setOf(1L))

        service.record(1L, LOCATION)

        verify(userLastLocationPersistencePort).save(1L, LOCATION)
    }

    @Test
    fun `탈퇴했거나 없는 사용자면 저장하지 않는다`() {
        `when`(userPersistencePort.lockActive(listOf(1L))).thenReturn(emptySet())

        service.record(1L, LOCATION)

        verifyNoInteractions(userLastLocationPersistencePort)
    }

    private companion object {
        val LOCATION = Coordinate(37.544, 127.056)
    }
}
