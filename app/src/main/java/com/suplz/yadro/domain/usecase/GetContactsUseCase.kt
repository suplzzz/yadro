package com.suplz.yadro.domain.usecase

import com.suplz.yadro.domain.model.Contact
import com.suplz.yadro.domain.repository.ContactsRepository
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(): List<Contact> {
        return repository.getContacts()
    }
}