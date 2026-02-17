package id.usecase.noted.presentation.note.editor.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.usecase.noted.domain.NoteVisibility
import id.usecase.noted.util.QrCodeGenerator

@Composable
fun ShareDialog(
    noteId: String,
    shareLink: String,
    visibility: NoteVisibility,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showQrCode by remember { mutableStateOf(false) }

    LaunchedEffect(shareLink) {
        if (shareLink.isNotBlank()) {
            qrCodeBitmap = QrCodeGenerator.generateQrCode(shareLink, 512, 512)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Note") },
        text = {
            if (showQrCode && qrCodeBitmap != null) {
                QrCodeView(
                    qrCodeBitmap = qrCodeBitmap!!,
                    onBack = { showQrCode = false },
                )
            } else {
                ShareDialogContent(
                    shareLink = shareLink,
                    visibility = visibility,
                    qrCodeBitmap = qrCodeBitmap,
                    onCopyLink = { copyToClipboard(context, shareLink) },
                    onShare = { shareLink(context, shareLink) },
                    onShowQrCode = { showQrCode = true },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun ShareDialogContent(
    shareLink: String,
    visibility: NoteVisibility,
    qrCodeBitmap: Bitmap?,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
    onShowQrCode: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VisibilityMessage(visibility = visibility)

        if (shareLink.isNotBlank()) {
            LinkDisplay(
                shareLink = shareLink,
                onCopyLink = onCopyLink,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Share")
                }

                if (qrCodeBitmap != null) {
                    IconButton(onClick = onShowQrCode) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Show QR Code",
                        )
                    }
                }
            }
        } else {
            Text(
                text = "No share link available. Make the note visible by changing its visibility setting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VisibilityMessage(visibility: NoteVisibility) {
    val message = when (visibility) {
        NoteVisibility.PRIVATE -> "This note is private. Only you can access it."
        NoteVisibility.LINK_SHARED -> "This note is shared via link. Anyone with the link can view it."
        NoteVisibility.PUBLIC -> "This note is public and can be discovered by anyone."
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LinkDisplay(
    shareLink: String,
    onCopyLink: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = shareLink,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        IconButton(onClick = onCopyLink) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Copy link",
            )
        }
    }
}

@Composable
private fun QrCodeView(
    qrCodeBitmap: Bitmap,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Scan to open note",
            style = MaterialTheme.typography.titleMedium,
        )
        Image(
            bitmap = qrCodeBitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier.size(256.dp),
        )
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Note Link", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareLink(context: Context, link: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, link)
        putExtra(Intent.EXTRA_SUBJECT, "Shared Note")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share note via"))
}
