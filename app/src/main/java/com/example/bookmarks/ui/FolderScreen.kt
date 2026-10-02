package com.example.bookmarks.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookmarks.R
import com.example.bookmarks.data.Bookmark
import com.example.bookmarks.data.Folder
import com.example.bookmarks.data.descendantsAndSelf
import com.example.bookmarks.data.hostOf
import kotlinx.coroutines.launch

/** A link received from another app through the share sheet. */
data class SharedLink(val url: String, val title: String)

private sealed interface FolderDialog {
    data object NewFolder : FolderDialog
    data class EditFolder(val folder: Folder) : FolderDialog
    data class MoveFolder(val folder: Folder) : FolderDialog
    data class DeleteFolder(val folder: Folder) : FolderDialog
    data class EditBookmark(val bookmark: Bookmark?) : FolderDialog
    data class MoveBookmark(val bookmark: Bookmark) : FolderDialog
}

/**
 * Shows the subfolders and links of one folder ([folderId] null = top level).
 * When [sharedLink] is set, the "save link" dialog opens immediately with it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    folderId: Long?,
    onOpenFolder: (Long) -> Unit,
    onBack: (() -> Unit)?,
    sharedLink: SharedLink? = null,
    onSharedLinkHandled: (saved: Boolean) -> Unit = {},
    viewModel: FolderViewModel = viewModel(factory = FolderViewModel.factory(folderId)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<FolderDialog?>(null) }

    val linkDeleted = stringResource(R.string.link_deleted)
    val undo = stringResource(R.string.undo)
    val accent = folderColor(state.folder)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PathText(
                                state.path,
                                MaterialTheme.typography.titleLarge,
                                Modifier.weight(1f, fill = false),
                            )
                            BlinkingCursor(color = accent, style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            stringResource(R.string.counts, state.subfolders.size, state.bookmarks.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { dialog = FolderDialog.NewFolder }) {
                        Icon(painterResource(R.drawable.ic_create_new_folder), stringResource(R.string.new_folder))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialog = FolderDialog.EditBookmark(null) },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.add_link)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.loading -> Unit
            state.subfolders.isEmpty() && state.bookmarks.isEmpty() ->
                EmptyFolder(Modifier.padding(innerPadding))
            // Folders are square tiles, two per row; headers and link cards span the full width.
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = innerPadding.calculateTopPadding(),
                    // Leave room so the FAB never covers the last item.
                    bottom = innerPadding.calculateBottomPadding() + 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.subfolders.isNotEmpty()) {
                    item(key = "folders-header", span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(stringResource(R.string.section_folders))
                    }
                }
                items(state.subfolders, key = { "folder-${it.id}" }) { folder ->
                    FolderTile(
                        folder = folder,
                        subfolders = state.subfolderCounts[folder.id] ?: 0,
                        links = state.bookmarkCounts[folder.id] ?: 0,
                        onClick = { onOpenFolder(folder.id) },
                        onEdit = { dialog = FolderDialog.EditFolder(folder) },
                        onMove = { dialog = FolderDialog.MoveFolder(folder) },
                        onDelete = { dialog = FolderDialog.DeleteFolder(folder) },
                    )
                }
                if (state.bookmarks.isNotEmpty()) {
                    item(key = "links-header", span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(stringResource(R.string.section_links))
                    }
                }
                items(
                    state.bookmarks,
                    key = { "bookmark-${it.id}" },
                    span = { GridItemSpan(maxLineSpan) },
                ) { bookmark ->
                    LinkRow(
                        bookmark = bookmark,
                        accent = accent,
                        onClick = { context.openUrl(bookmark.url) },
                        onEdit = { dialog = FolderDialog.EditBookmark(bookmark) },
                        onMove = { dialog = FolderDialog.MoveBookmark(bookmark) },
                        onCopy = { context.copyUrl(bookmark.url) },
                        onShare = { context.shareUrl(bookmark) },
                        onRefresh = { viewModel.refreshPreview(bookmark) },
                        onDelete = {
                            viewModel.deleteBookmark(bookmark)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    linkDeleted,
                                    actionLabel = undo,
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) viewModel.restoreBookmark(bookmark)
                            }
                        },
                    )
                }
            }
        }
    }

    val close = { dialog = null }
    when (val d = dialog) {
        null -> Unit
        FolderDialog.NewFolder -> FolderEditDialog(
            command = stringResource(R.string.cmd_new_folder),
            confirmLabel = stringResource(R.string.create),
            initialName = "",
            initialColor = null,
            onConfirm = { name, color -> viewModel.createFolder(name, color); close() },
            onDismiss = close,
        )
        is FolderDialog.EditFolder -> FolderEditDialog(
            command = stringResource(R.string.cmd_edit_folder),
            confirmLabel = stringResource(R.string.save),
            initialName = d.folder.name,
            initialColor = d.folder.color,
            onConfirm = { name, color -> viewModel.editFolder(d.folder, name, color); close() },
            onDismiss = close,
        )
        is FolderDialog.MoveFolder -> FolderPickerDialog(
            command = stringResource(R.string.cmd_move),
            allFolders = state.allFolders,
            selectedId = d.folder.parentId,
            // A folder cannot be moved inside itself or one of its own subfolders.
            excludedIds = remember(d, state.allFolders) { descendantsAndSelf(d.folder.id, state.allFolders) },
            onPick = { viewModel.moveFolder(d.folder, it); close() },
            onDismiss = close,
        )
        is FolderDialog.DeleteFolder -> AlertDialog(
            onDismissRequest = close,
            title = { CommandTitle(stringResource(R.string.cmd_delete_folder, d.folder.name)) },
            text = { Text(stringResource(R.string.delete_folder_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFolder(d.folder); close() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } },
        )
        is FolderDialog.EditBookmark -> BookmarkDialog(
            command = stringResource(if (d.bookmark == null) R.string.cmd_add_link else R.string.cmd_edit_link),
            initialUrl = d.bookmark?.url.orEmpty(),
            initialTitle = d.bookmark?.title.orEmpty(),
            initialFolderId = d.bookmark?.folderId ?: folderId,
            allFolders = state.allFolders,
            onSave = { url, title, targetFolder ->
                viewModel.saveBookmark(d.bookmark, url, title, targetFolder)
                close()
            },
            onDismiss = close,
        )
        is FolderDialog.MoveBookmark -> FolderPickerDialog(
            command = stringResource(R.string.cmd_move),
            allFolders = state.allFolders,
            selectedId = d.bookmark.folderId,
            onPick = { viewModel.moveBookmark(d.bookmark, it); close() },
            onDismiss = close,
        )
    }

    if (sharedLink != null && !state.loading) {
        BookmarkDialog(
            command = stringResource(R.string.cmd_add_link),
            initialUrl = sharedLink.url,
            initialTitle = sharedLink.title,
            initialFolderId = folderId,
            allFolders = state.allFolders,
            onSave = { url, title, targetFolder ->
                scope.launch {
                    // Wait for the write: the activity may close right after.
                    viewModel.saveBookmark(null, url, title, targetFolder).join()
                    onSharedLinkHandled(true)
                }
            },
            onDismiss = { onSharedLinkHandled(false) },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp),
    )
}

/** Big square tile with a large folder drawing, tinted with the folder's color. */
@Composable
private fun FolderTile(
    folder: Folder,
    subfolders: Int,
    links: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = folderColor(folder)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        Icon(
            painterResource(R.drawable.ic_folder),
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 36.dp)
                .fillMaxSize(0.62f),
        )
        OverflowMenu(
            stringResource(R.string.edit) to onEdit,
            stringResource(R.string.move) to onMove,
            stringResource(R.string.delete) to onDelete,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            Text(
                buildAnnotatedString {
                    append(folder.name)
                    withStyle(SpanStyle(color = color)) { append("/") }
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.counts, subfolders, links),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** A saved link: the page title and its address. */
@Composable
private fun LinkRow(
    bookmark: Bookmark,
    accent: Color,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(accent.copy(alpha = 0.16f), MaterialTheme.shapes.medium),
        ) {
            Icon(
                painterResource(R.drawable.ic_link),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                bookmark.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                bookmark.url.substringAfter("://"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OverflowMenu(
            stringResource(R.string.edit) to onEdit,
            stringResource(R.string.move) to onMove,
            stringResource(R.string.copy_url) to onCopy,
            stringResource(R.string.share) to onShare,
            stringResource(R.string.refresh_preview) to onRefresh,
            stringResource(R.string.delete) to onDelete,
        )
    }
}

/** Terminal-style empty state. */
@Composable
private fun EmptyFolder(modifier: Modifier = Modifier) {
    val prompt = MaterialTheme.colorScheme.primary
    val comment = MaterialTheme.colorScheme.tertiary
    val style = MaterialTheme.typography.bodyMedium
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = prompt, fontWeight = FontWeight.Bold)) { append("$ ") }
                        append(stringResource(R.string.empty_ls))
                    },
                    style = style,
                )
                Text(
                    stringResource(R.string.empty_result),
                    style = style,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.empty_hint_add), style = style, color = comment)
                Text(stringResource(R.string.empty_hint_share), style = style, color = comment)
                Spacer(Modifier.size(8.dp))
                Row {
                    Text("$ ", style = style, color = prompt, fontWeight = FontWeight.Bold)
                    BlinkingCursor(style = style)
                }
            }
        }
    }
}

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.no_app_to_open, Toast.LENGTH_SHORT).show()
    }
}

private fun Context.copyUrl(url: String) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("url", url))
    // Android 13+ already shows its own confirmation.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, R.string.url_copied, Toast.LENGTH_SHORT).show()
    }
}

private fun Context.shareUrl(bookmark: Bookmark) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, bookmark.title)
        .putExtra(Intent.EXTRA_TEXT, bookmark.url)
    startActivity(Intent.createChooser(send, null))
}
