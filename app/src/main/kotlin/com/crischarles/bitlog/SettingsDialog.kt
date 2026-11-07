@file:OptIn(ExperimentalMaterial3Api::class)

package com.crischarles.bitlog

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Lets the user pick a background image and a ringtone. Each can be reset to the built-in default. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(settings: AppSettings, onDismiss: () -> Unit) {
    val context = LocalContext.current

    // Keep read access to the picked file across app restarts, when the provider allows it.
    fun keepReadAccess(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // The provider refused a persistable grant; the file stays readable for this session only.
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            keepReadAccess(uri)
            settings.setBackground(uri.toString())
        }
    }
    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            keepReadAccess(uri)
            settings.setRingtone(uri.toString())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Background", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (settings.backgroundUri.isEmpty()) "Abstract default" else "Custom image",
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = { pickImage.launch(arrayOf("image/*")) }) { Text("Choose image") }
                        if (settings.backgroundUri.isNotEmpty()) {
                            TextButton(onClick = { settings.setBackground("") }) { Text("Reset") }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ringtone", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (settings.ringtoneUri.isEmpty()) "Built-in bell" else "Custom sound",
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = { pickSound.launch(arrayOf("audio/*")) }) { Text("Choose sound") }
                        if (settings.ringtoneUri.isNotEmpty()) {
                            TextButton(onClick = { settings.setRingtone("") }) { Text("Reset") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}
