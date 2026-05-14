package com.suplz.yadro.domain.repository

import com.suplz.yadro.domain.model.Contact
import com.suplz.yadro.domain.model.DeDuplicationResult

interface ContactsRepository {

    suspend fun getContacts(): List<Contact>

    suspend fun removeDuplicates(): DeDuplicationResult
}