package com.example.backend.media.application.event

import com.example.backend.media.application.port.outbound.MediaStoragePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrphanMediaDeletionListenerTest {
    private val deletedKeys = mutableListOf<String>()

    private val fakePort =
        object : MediaStoragePort {
            override fun presignedPutUrl(
                key: String,
                contentType: String,
                contentLength: Long,
            ): String = error("사용하지 않음")

            override fun publicUrl(key: String): String = error("사용하지 않음")

            override fun delete(key: String) {
                deletedKeys.add(key)
            }
        }

    private val listener = OrphanMediaDeletionListener(fakePort)

    @Test
    fun `이벤트를 받으면 해당 key의 S3 객체를 삭제한다`() {
        listener.handle(OrphanMediaDeletionRequested("profile/1/uuid.jpg"))

        assertEquals(listOf("profile/1/uuid.jpg"), deletedKeys)
    }
}
