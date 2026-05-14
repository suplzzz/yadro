package com.suplz.yadro.domain.usecase

import com.suplz.yadro.domain.model.DeDuplicationResult
import com.suplz.yadro.domain.repository.ContactsRepository
import javax.inject.Inject

class RemoveDuplicatesUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(): DeDuplicationResult {
        return repository.removeDuplicates()
    }
}