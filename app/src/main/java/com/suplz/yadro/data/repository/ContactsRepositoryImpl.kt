package com.suplz.yadro.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.suplz.yadro.IContactsDeDuplicator
import com.suplz.yadro.IDeDuplicationCallback
import com.suplz.yadro.domain.model.Contact
import com.suplz.yadro.domain.model.DeDuplicationResult
import com.suplz.yadro.domain.repository.ContactsRepository
import com.suplz.yadro.data.service.ContactsService
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ContactsRepositoryImpl @Inject constructor(
    private val context: Context
) : ContactsRepository {

    override suspend fun getContacts(): List<Contact> = suspendCancellableCoroutine { continuation ->
        val connection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val currentConnection = this
                val deDuplicator = IContactsDeDuplicator.Stub.asInterface(service)

                try {
                    val aidlContacts = deDuplicator.getContacts()

                    val domainContacts = aidlContacts.map { aidlContact ->
                        Contact(
                            id = aidlContact.id,
                            name = aidlContact.name,
                            phoneNumber = aidlContact.phoneNumber
                        )
                    }

                    context.unbindService(currentConnection)
                    continuation.resume(domainContacts)
                } catch (e: Exception) {
                    context.unbindService(currentConnection)
                    continuation.resumeWithException(e)
                }
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                if (continuation.isActive) {
                    continuation.resumeWithException(RuntimeException("Service disconnected"))
                }
            }
        }

        val intent = Intent(context, ContactsService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        continuation.invokeOnCancellation {
            try {
                context.unbindService(connection)
            } catch (e: IllegalArgumentException) {

            }
        }
    }

    override suspend fun removeDuplicates(): DeDuplicationResult = suspendCancellableCoroutine { continuation ->
        val connection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val currentConnection = this
                val deDuplicator = IContactsDeDuplicator.Stub.asInterface(service)

                deDuplicator.removeDuplicates(object : IDeDuplicationCallback.Stub() {
                    override fun onSuccess() {
                        context.unbindService(currentConnection)
                        continuation.resume(DeDuplicationResult.SUCCESS)
                    }

                    override fun onError(message: String?) {
                        context.unbindService(currentConnection)
                        continuation.resume(DeDuplicationResult.ERROR)
                    }

                    override fun onNoDuplicatesFound() {
                        context.unbindService(currentConnection)
                        continuation.resume(DeDuplicationResult.NO_DUPLICATES)
                    }
                })
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                if (continuation.isActive) {
                    continuation.resume(DeDuplicationResult.ERROR)
                }
            }
        }

        val intent = Intent(context, ContactsService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        continuation.invokeOnCancellation {
            try {
                context.unbindService(connection)
            } catch (e: IllegalArgumentException) {

            }
        }
    }
}