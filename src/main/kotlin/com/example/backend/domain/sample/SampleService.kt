package com.example.backend.domain.sample

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SampleService(
    private val repository: SampleRepository,
) {
    fun list(): List<Sample> = repository.findAll()

    @Transactional
    fun create(name: String): Sample {
        val id = repository.create(name)
        return Sample(id, name)
    }
}
