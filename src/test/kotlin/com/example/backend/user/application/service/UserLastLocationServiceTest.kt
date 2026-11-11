package com.example.backend.user.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.outbound.UserLastLocationPersistencePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class UserLastLocationServiceTest {
    private val userLastLocationPersistencePort = mock(UserLastLocationPersistencePort::class.java)
    private val userPersistencePort = mock(UserPersistencePort::class.java)
    private val service = UserLastLocationService(userLastLocationPersistencePort, userPersistencePort)

    @Test
    fun `활성 사용자면 최근 위치를 소수 3자리로 반올림해 저장한다`() {
        `when`(userPersistencePort.lockActiveShared(listOf(1L))).thenReturn(setOf(1L))

        service.record(1L, Coordinate(37.5445678, 127.0561234))

        verify(userLastLocationPersistencePort).save(1L, Coordinate(37.545, 127.056))
    }

    @Test
    fun `최근에 같은 자리로 저장됐으면 잠그지도 쓰지도 않는다`() {
        `when`(userLastLocationPersistencePort.isRecentNear(1L, Coordinate(37.545, 127.056))).thenReturn(true)

        service.record(1L, Coordinate(37.5445678, 127.0561234))

        verifyNoInteractions(userPersistencePort)
        verify(userLastLocationPersistencePort, never()).save(anyLong(), anyCoordinate())
    }

    @Test
    fun `활성 사용자가 아니면 저장하지 않는다`() {
        service.record(2L, Coordinate(37.5445678, 127.0561234))

        verify(userLastLocationPersistencePort, never()).save(anyLong(), anyCoordinate())
    }
}

/** Kotlin 에서 null 검사 없이 쓰는 Mockito any(). mockito-kotlin 과 같은 방식이다. */
private fun anyCoordinate(): Coordinate = org.mockito.Mockito.any(Coordinate::class.java) ?: Coordinate(0.0, 0.0)
