package com.soobinpark.appcraft.readingbible.feature.personalnotes

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soobinpark.appcraft.readingbible.R
import com.soobinpark.appcraft.readingbible.domain.model.PersonalNote
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

private val FloatingTabClearance = 76.dp
private val SwipeActionWidth = 112.dp

@Composable
fun PersonalNotesRoute(
    notes: List<PersonalNote>,
    onAddNote: (String, String) -> Unit,
    onUpdateNote: (PersonalNote) -> Unit,
    onDeleteNote: (String) -> Unit,
    onExportNotes: (Set<String>) -> String,
    onImportNotes: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var revealedNoteId by remember { mutableStateOf<String?>(null) }
    var editingNote by remember { mutableStateOf<PersonalNote?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        val payload = pendingExport
        pendingExport = null
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(payload.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                Toast.makeText(context, context.getString(R.string.personal_notes_backup_success), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.personal_notes_backup_failure), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }.onSuccess { raw ->
                runCatching {
                    onImportNotes(raw)
                    selectedIds = emptySet()
                    Toast.makeText(context, context.getString(R.string.personal_notes_restore_success), Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(context, error.message ?: context.getString(R.string.personal_notes_restore_failure), Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.personal_notes_file_read_failure), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Text(stringResource(R.string.tab_personal_notes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = stringResource(R.string.personal_notes_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        if (selectedIds.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.personal_notes_select_for_backup), Toast.LENGTH_SHORT).show()
                        } else {
                            pendingExport = onExportNotes(selectedIds)
                            exportLauncher.launch(personalNotesBackupFilename())
                        }
                    },
                    enabled = selectedIds.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.action_backup))
                }
                TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream")) }) {
                    Text(stringResource(R.string.action_restore))
                }
                if (selectedIds.isNotEmpty()) {
                    TextButton(onClick = { selectedIds = emptySet() }) {
                        Text(stringResource(R.string.personal_notes_clear_count, selectedIds.size))
                    }
                }
            }
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = FloatingTabClearance + 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(stringResource(R.string.personal_notes_empty_title), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        Text(
                            stringResource(R.string.personal_notes_empty_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = FloatingTabClearance + 72.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(notes, key = { it.id }) { note ->
                        PersonalNoteRow(
                            note = note,
                            selected = selectedIds.contains(note.id),
                            revealed = revealedNoteId == note.id,
                            onReveal = { revealedNoteId = note.id },
                            onCollapse = { if (revealedNoteId == note.id) revealedNoteId = null },
                            onClick = {
                                if (revealedNoteId == note.id) {
                                    revealedNoteId = null
                                } else if (selectedIds.isEmpty()) {
                                    editingNote = note
                                } else {
                                    selectedIds = selectedIds.toggle(note.id)
                                }
                            },
                            onLongClick = { selectedIds = selectedIds.toggle(note.id) },
                            onEdit = {
                                revealedNoteId = null
                                editingNote = note
                            },
                            onDelete = {
                                revealedNoteId = null
                                onDeleteNote(note.id)
                                selectedIds = selectedIds - note.id
                            },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = FloatingTabClearance),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.personal_notes_add))
        }
    }

    editingNote?.let { note ->
        PersonalNoteEditorDialog(
            title = stringResource(R.string.personal_notes_edit_title),
            initialTitle = note.title,
            initialBody = note.body,
            onDismiss = { editingNote = null },
            onSave = { title, body ->
                onUpdateNote(note.copy(title = title, body = body, updatedAtMillis = System.currentTimeMillis()))
                editingNote = null
            },
        )
    }

    if (showCreateDialog) {
        PersonalNoteEditorDialog(
            title = stringResource(R.string.personal_notes_new_title),
            initialTitle = "",
            initialBody = "",
            onDismiss = { showCreateDialog = false },
            onSave = { title, body ->
                onAddNote(title, body)
                showCreateDialog = false
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonalNoteRow(
    note: PersonalNote,
    selected: Boolean,
    revealed: Boolean,
    onReveal: () -> Unit,
    onCollapse: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { SwipeActionWidth.toPx() }
    var offsetX by remember(note.id) { mutableFloatStateOf(0f) }

    LaunchedEffect(revealed) {
        offsetX = if (revealed) -actionWidthPx else 0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(SwipeActionWidth)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .background(Color(0xFFE53935), RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = Color.White,
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                        )
                    } else {
                        Modifier
                    },
                )
                .pointerInput(note.id, revealed) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = if (offsetX <= -actionWidthPx / 2f) {
                                onReveal()
                                -actionWidthPx
                            } else {
                                onCollapse()
                                0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-actionWidthPx, 0f)
                        },
                    )
                }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.personal_notes_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.body.isNotBlank()) {
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(note.updatedAtMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PersonalNoteEditorDialog(
    title: String,
    initialTitle: String,
    initialBody: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var noteTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
    var noteBody by remember(initialBody) { mutableStateOf(initialBody) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text(stringResource(R.string.field_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = noteBody,
                        onValueChange = { noteBody = it },
                        label = { Text(stringResource(R.string.field_content)) },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        TextButton(onClick = { onSave(noteTitle, noteBody) }) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }
        }
    }
}

private fun Set<String>.toggle(id: String): Set<String> {
    return if (contains(id)) this - id else this + id
}

private fun personalNotesBackupFilename(): String {
    val stamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
        .format(Date())
        .replace(":", "-")
        .replace("/", "-")
    return "personal-notes-$stamp.rmb-notes.json"
}
