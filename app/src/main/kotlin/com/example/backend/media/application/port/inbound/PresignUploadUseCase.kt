package com.example.backend.media.application.port.inbound

import com.example.backend.media.application.port.inbound.dto.PresignCommand
import com.example.backend.media.application.port.inbound.dto.PresignResult

interface PresignUploadUseCase {
    fun presign(command: PresignCommand): List<PresignResult>
}
