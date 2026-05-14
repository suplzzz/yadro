package com.suplz.yadro.presentation

import android.Manifest
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.suplz.yadro.R
import com.suplz.yadro.presentation.ui.theme.AvatarColors
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val events = viewModel.events
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveEvents(
        events = events,
        snackbarHostState = snackbarHostState
    )

    val permissionMsg = stringResource(id = R.string.msg_permissions_required)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_CONTACTS] ?: false

        if (readGranted && writeGranted) {
            viewModel.loadContacts()
        } else {
            Toast.makeText(
                context,
                permissionMsg,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.CALL_PHONE
            )
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_contacts)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.removeDuplicates() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = stringResource(R.string.btn_delete_duplicates),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.groupedContacts.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.groupedContacts.isEmpty()) {
                Text(
                    text = stringResource(R.string.msg_no_contacts),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.groupedContacts.forEach { (initial, contacts) ->
                        item(key = "header_$initial") {
                            Text(
                                text = initial.toString(),
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }

                        itemsIndexed(
                            items = contacts,
                            key = { index, _ -> "${initial}_${index}" }
                        ) { index, uiModel ->
                            val isFirst = index == 0
                            val isLast = index == contacts.lastIndex

                            val shape = when {
                                isFirst && isLast -> RoundedCornerShape(16.dp)
                                isFirst -> RoundedCornerShape(
                                    topStart = 16.dp, topEnd = 16.dp,
                                    bottomStart = 4.dp, bottomEnd = 4.dp
                                )
                                isLast -> RoundedCornerShape(
                                    topStart = 4.dp, topEnd = 4.dp,
                                    bottomStart = 16.dp, bottomEnd = 16.dp
                                )
                                else -> RoundedCornerShape(4.dp)
                            }

                            ContactItem(
                                uiModel = uiModel,
                                shape = shape,
                                showDivider = !isLast,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_CALL).apply {
                                        data = "tel:${uiModel.contact.phoneNumber}".toUri()
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObserveEvents(
    events: Flow<ContactsEvent>,
    snackbarHostState: SnackbarHostState,
) {
    val msgDeleting = stringResource(R.string.msg_deleting)
    val msgSuccess = stringResource(R.string.msg_duplicates_deleted)
    val msgNoDuplicates = stringResource(R.string.msg_no_duplicates)
    val msgError = stringResource(R.string.msg_error_occurred)
    val msgGenericTemplate = stringResource(R.string.msg_generic_error)

    LaunchedEffect(Unit) {
        events.collect { event ->
            val textToShow = when (event) {
                is ContactsEvent.DeletingStarted -> msgDeleting
                is ContactsEvent.DuplicatesDeleted -> msgSuccess
                is ContactsEvent.NoDuplicatesFound -> msgNoDuplicates
                is ContactsEvent.DeletionError -> msgError
                is ContactsEvent.ErrorWithMessage -> {
                    if (event.errorMsg.isNullOrBlank()) {
                        msgError
                    } else {
                        msgGenericTemplate.format(event.errorMsg)
                    }
                }
            }

            snackbarHostState.showSnackbar(
                message = textToShow,
                withDismissAction = true
            )
        }
    }
}

@Composable
fun ContactItem(
    uiModel: ContactUiModel,
    shape: Shape,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val displayName = uiModel.contact.name.ifBlank {
        stringResource(R.string.unknown_contact)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(name = displayName)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (uiModel.contact.phoneNumber.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiModel.contact.phoneNumber,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiModel.isDuplicate) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.duplicate),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp),
                thickness = 0.1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ContactAvatar(name: String) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val color = remember(name) {
        AvatarColors[abs(name.hashCode()) % AvatarColors.size]
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}