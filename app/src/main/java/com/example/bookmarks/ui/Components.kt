package com.example.bookmarks.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bookmarks.R
import com.example.bookmarks.data.Folder

/** The folder's own color, or the theme's brown when it has none. */
@Composable
fun folderColor(folder: Folder?): Color =
    folder?.color?.let(::Color) ?: MaterialTheme.colorScheme.primary

@Composable
fun folderColor(argb: Int?): Color = argb?.let(::Color) ?: MaterialTheme.colorScheme.primary

/** Folder glyph on a tinted square, like file icons in an IDE sidebar. */
@Composable
fun FolderBadge(color: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = 0.16f), MaterialTheme.shapes.medium),
    ) {
        Icon(
            painterResource(R.drawable.ic_folder),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** "~/Lavoro/Progetti", with the "~" and slashes in the accent color. */
@Composable
fun PathText(path: List<Folder>, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append("~") }
            path.forEachIndexed { index, folder ->
                withStyle(SpanStyle(color = accent)) { append("/") }
                val last = index == path.lastIndex
                withStyle(
                    SpanStyle(
                        color = if (last) MaterialTheme.colorScheme.onSurface else muted,
                        fontWeight = if (last) FontWeight.Bold else FontWeight.Normal,
                    ),
                ) { append(folder.name) }
            }
        },
        style = style,
        maxLines = 1,
        softWrap = false,
        // Keep the current folder visible when the path is too long.
        overflow = TextOverflow.StartEllipsis,
        modifier = modifier,
    )
}

/** Blinking terminal cursor. */
@Composable
fun BlinkingCursor(color: Color = MaterialTheme.colorScheme.primary, style: androidx.compose.ui.text.TextStyle) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(530, easing = LinearEasing), RepeatMode.Reverse),
        label = "cursorAlpha",
    )
    Text("▍", color = color, style = style, modifier = Modifier.alpha(if (alpha > 0.5f) 1f else 0f))
}

/** Dialog title written as a shell command: "$ mkdir". */
@Composable
fun CommandTitle(command: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append("$ ")
            }
            append(command)
        },
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
fun OverflowMenu(
    vararg actions: Pair<String, () -> Unit>,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painterResource(R.drawable.ic_more_vert),
                stringResource(R.string.more_options),
                tint = tint,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        action()
                    },
                )
            }
        }
    }
}
