package com.example.backend.media.application.port.inbound

import com.example.backend.media.application.port.inbound.dto.PresignBatchCommand
import com.example.backend.media.application.port.inbound.dto.PresignCommand
import com.example.backend.media.application.port.inbound.dto.PresignResult

interface PresignUploadUseCase {
    fun presign(command: PresignCommand): PresignResult

    fun presignBatch(command: PresignBatchCommand): List<PresignResult>
}
