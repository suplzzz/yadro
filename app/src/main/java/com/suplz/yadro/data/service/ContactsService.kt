package com.suplz.yadro.data.service

import android.app.Service
import android.content.ContentProviderOperation
import android.content.Intent
import android.os.IBinder
import android.provider.ContactsContract
import com.suplz.yadro.IContactsDeDuplicator
import com.suplz.yadro.IDeDuplicationCallback
import com.suplz.yadro.data.model.AidlContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ContactsService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val binder = object : IContactsDeDuplicator.Stub() {

        override fun getContacts(): List<AidlContact> {
            val uniqueContacts = mutableMapOf<String, AidlContact>()

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val contactId = it.getString(idIndex) ?: continue

                    if (!uniqueContacts.containsKey(contactId)) {
                        val name = it.getString(nameIndex) ?: ""

                        val phone = it.getString(numberIndex) ?: ""
                        uniqueContacts[contactId] = AidlContact(id = contactId, name = name, phoneNumber = phone)
                    }
                }
            }
            return uniqueContacts.values.toList()
        }

        // УДАЛЕНИЕ ДУБЛИКАТОВ
        override fun removeDuplicates(callback: IDeDuplicationCallback?) {
            serviceScope.launch {
                try {
                    val hasDuplicates = performDeDuplication()

                    if (hasDuplicates) {
                        callback?.onSuccess()
                    } else {
                        callback?.onNoDuplicatesFound()
                    }
                } catch (e: SecurityException) {
                    callback?.onError("Permission denied: WRITE_CONTACTS")
                } catch (e: Exception) {
                    callback?.onError(e.message ?: "Unknown error occurred")
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun performDeDuplication(): Boolean {
        val visibleContacts = binder.contacts

        val groupedContacts = visibleContacts.groupBy { "${it.name}_${it.phoneNumber}" }
        val contactIdsToDelete = mutableListOf<String>()

        for ((_, contactsInGroup) in groupedContacts) {
            if (contactsInGroup.size > 1) {

                val duplicates = contactsInGroup.drop(1)
                contactIdsToDelete.addAll(duplicates.map { it.id })
            }
        }

        if (contactIdsToDelete.isEmpty()) return false

        val operations = ArrayList<ContentProviderOperation>()
        for (contactId in contactIdsToDelete) {
            operations.add(
                ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                    .withSelection("${ContactsContract.RawContacts.CONTACT_ID} = ?", arrayOf(contactId))
                    .build()
            )
        }

        return try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}