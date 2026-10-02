package com.example.bookmarks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bookmarks.R
import com.example.bookmarks.data.Folder
import com.example.bookmarks.data.flattenTree
import com.example.bookmarks.data.isValidUrl
import com.example.bookmarks.data.normalizeUrl
import com.example.bookmarks.data.pathTo
import com.example.bookmarks.ui.theme.FolderColors

/** Name + color of a folder, used both to create and to edit one. */
@Composable
fun FolderEditDialog(
    command: String,
    confirmLabel: String,
    initialName: String,
    initialColor: Int?,
    onConfirm: (name: String, color: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var color by rememberSaveable { mutableStateOf(initialColor) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { CommandTitle(command) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.folder_name)) },
                    leadingIcon = { FolderBadge(folderColor(color), size = 32.dp) },
                    suffix = { Text("/") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                Text(
                    stringResource(R.string.folder_color),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ColorPicker(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, color) }, enabled = name.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Theme default (brown) followed by [FolderColors], five per row. */
@Composable
private fun ColorPicker(selected: Int?, onSelect: (Int?) -> Unit) {
    val options: List<Int?> = listOf(null) + FolderColors.map { it.toArgb() }
    val defaultLabel = stringResource(R.string.default_color)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape,
                            )
                            .padding(4.dp)
                            .background(folderColor(option), CircleShape)
                            .clickable { onSelect(option) }
                            .semantics {
                                this.selected = isSelected
                                if (option == null) contentDescription = defaultLabel
                            },
                    ) {
                        if (isSelected) {
                            val check = if (folderColor(option).luminance() > 0.5f) Color.Black else Color.White
                            Text("✓", color = check, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkDialog(
    command: String,
    initialUrl: String,
    initialTitle: String,
    initialFolderId: Long?,
    allFolders: List<Folder>,
    onSave: (url: String, title: String, folderId: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by rememberSaveable { mutableStateOf(initialUrl) }
    var linkTitle by rememberSaveable { mutableStateOf(initialTitle) }
    var folderId by rememberSaveable { mutableStateOf(initialFolderId) }
    var pickingFolder by rememberSaveable { mutableStateOf(false) }
    val valid = isValidUrl(url)
    val path = remember(folderId, allFolders) { pathTo(folderId, allFolders.associateBy { it.id }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { CommandTitle(command) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.url)) },
                    singleLine = true,
                    isError = url.isNotBlank() && !valid,
                    supportingText = if (url.isNotBlank() && !valid) {
                        { Text(stringResource(R.string.invalid_url)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = linkTitle,
                    onValueChange = { linkTitle = it },
                    label = { Text(stringResource(R.string.title_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.folder_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                OutlinedButton(
                    onClick = { pickingFolder = true },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_folder),
                        contentDescription = null,
                        tint = folderColor(path.lastOrNull()),
                    )
                    Spacer(Modifier.width(8.dp))
                    PathText(path, MaterialTheme.typography.bodyMedium, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(normalizeUrl(url), linkTitle, folderId) }, enabled = valid) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )

    if (pickingFolder) {
        FolderPickerDialog(
            command = stringResource(R.string.cmd_choose_folder),
            allFolders = allFolders,
            selectedId = folderId,
            onPick = { folderId = it; pickingFolder = false },
            onDismiss = { pickingFolder = false },
        )
    }
}

/** Lets the user pick any folder in the tree (or the top level), except those in [excludedIds]. */
@Composable
fun FolderPickerDialog(
    command: String,
    allFolders: List<Folder>,
    selectedId: Long?,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
    excludedIds: Set<Long> = emptySet(),
) {
    val nodes = remember(allFolders, excludedIds) {
        flattenTree(allFolders).filter { it.folder.id !in excludedIds }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { CommandTitle(command) },
        text = {
            LazyColumn {
                item {
                    PickerRow(
                        name = "~",
                        color = MaterialTheme.colorScheme.primary,
                        depth = 0,
                        selected = selectedId == null,
                        onClick = { onPick(null) },
                    )
                }
                items(nodes, key = { it.folder.id }) { node ->
                    PickerRow(
                        name = node.folder.name + "/",
                        color = folderColor(node.folder),
                        depth = node.depth + 1,
                        selected = selectedId == node.folder.id,
                        onClick = { onPick(node.folder.id) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun PickerRow(name: String, color: Color, depth: Int, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .clickable(onClick = onClick)
            .padding(start = 8.dp + 16.dp * depth, top = 10.dp, bottom = 10.dp, end = 8.dp),
    ) {
        if (depth > 0) {
            Text(
                "└ ",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Icon(painterResource(R.drawable.ic_folder), contentDescription = null, tint = color)
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
