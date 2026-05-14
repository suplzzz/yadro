package com.suplz.yadro.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suplz.yadro.domain.model.Contact
import com.suplz.yadro.domain.model.DeDuplicationResult
import com.suplz.yadro.domain.usecase.GetContactsUseCase
import com.suplz.yadro.domain.usecase.RemoveDuplicatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed interface ContactsEvent {
    data object DeletingStarted : ContactsEvent
    data object DuplicatesDeleted : ContactsEvent
    data object NoDuplicatesFound : ContactsEvent
    data object DeletionError : ContactsEvent
    data class ErrorWithMessage(val errorMsg: String?) : ContactsEvent
}

data class ContactUiModel(
    val contact: Contact,
    val isDuplicate: Boolean
)

data class ContactsUiState(
    val isLoading: Boolean = false,
    val groupedContacts: Map<Char, List<ContactUiModel>> = emptyMap()

)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val removeDuplicatesUseCase: RemoveDuplicatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _events = Channel<ContactsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val contactsList = getContactsUseCase()
                _uiState.update { it.copy(isLoading = false, groupedContacts = groupContacts(contactsList)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.send(ContactsEvent.ErrorWithMessage(e.message))
            }
        }
    }

    fun removeDuplicates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            _events.send(ContactsEvent.DeletingStarted)

            try {
                val result = removeDuplicatesUseCase()

                when (result) {
                    DeDuplicationResult.SUCCESS -> _events.send(ContactsEvent.DuplicatesDeleted)
                    DeDuplicationResult.ERROR -> _events.send(ContactsEvent.DeletionError)
                    DeDuplicationResult.NO_DUPLICATES -> _events.send(ContactsEvent.NoDuplicatesFound)
                }

                val updatedContacts = getContactsUseCase()
                _uiState.update {
                    it.copy(isLoading = false, groupedContacts = groupContacts(updatedContacts))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.send(ContactsEvent.ErrorWithMessage(e.message))
            }
        }
    }

    private fun groupContacts(contacts: List<Contact>): Map<Char, List<ContactUiModel>> {
        val grouped = contacts.groupBy { "${it.name}_${it.phoneNumber}" }

        val duplicateIds = grouped.values
            .filter { it.size > 1 }
            .flatMap { it.drop(1) }
            .map { it.id }
            .toSet()

        val uiModels = contacts.map { contact ->
            ContactUiModel(
                contact = contact,
                isDuplicate = duplicateIds.contains(contact.id)
            )
        }

        return uiModels.groupBy { uiModel ->
            val firstChar = uiModel.contact.name.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar.isLetter()) firstChar else '#'
        }.toSortedMap()
    }
}