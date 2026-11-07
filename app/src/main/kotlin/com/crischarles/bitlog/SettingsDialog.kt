package com.crischarles.bitlog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

@Composable
fun SettingsDialog(
    settings: AppSettings,
    backdrop: LayerBackdrop,
    textColor: Color,
    secondaryTextColor: Color,
    surfaceColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    fun keepReadAccess(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Read access available for this session only if persistent grant denied
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

    var canOverlay by remember {
        mutableStateOf(OverlayPermissionHelper.canDrawOverlays(context))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val shape = RoundedCornerShape(32.dp)
            val dialogModifier = if (supportsLiquidGlass) {
                Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(18f.dp.toPx())
                            lens(20f.dp.toPx(), 40f.dp.toPx())
                        },
                        highlight = { Highlight.Default },
                        onDrawSurface = {
                            drawRect(surfaceColor.copy(alpha = 0.35f))
                        },
                    )
            } else {
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Color(0xFF2C2C2E).copy(alpha = 0.85f))
            }

            Column(
                modifier = dialogModifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* prevent tap through */ },
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiquidText(
                        text = "Settings",
                        style = AppTypography.Title1,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                    LiquidIconButton(
                        onClick = onDismiss,
                        backdrop = backdrop,
                        size = 32.dp,
                        surfaceColor = surfaceColor,
                    ) {
                        LiquidText(
                            text = "✕",
                            style = AppTypography.Callout,
                            color = textColor,
                        )
                    }
                }

                // NOTCH & BACKGROUND NOTIFICATIONS
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiquidText(
                        text = "Notifications & Notch (${OsDetector.currentFlavor.displayName})",
                        style = AppTypography.Headline,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )

                    // System Notification Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            LiquidText(
                                text = "Status Bar Notification",
                                style = AppTypography.Callout,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                            )
                            LiquidText(
                                text = "Live countdown & actions in notification bar",
                                style = AppTypography.Footnote,
                                color = secondaryTextColor,
                            )
                        }

                        LiquidButton(
                            onClick = {
                                settings.setSystemNotification(!settings.systemNotificationEnabled)
                            },
                            backdrop = backdrop,
                            surfaceColor = if (settings.systemNotificationEnabled) {
                                Color(0xFF34C759).copy(alpha = 0.85f)
                            } else surfaceColor,
                        ) {
                            LiquidText(
                                text = if (settings.systemNotificationEnabled) "On" else "Off",
                                style = AppTypography.Caption,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.systemNotificationEnabled) Color.White else secondaryTextColor,
                            )
                        }
                    }

                    // Notch Dynamic Island Notification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            LiquidText(
                                text = "Notch Dynamic Island",
                                style = AppTypography.Callout,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                            )
                            LiquidText(
                                text = "Floating capsule around camera notch when app is in background",
                                style = AppTypography.Footnote,
                                color = secondaryTextColor,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!canOverlay && settings.notchNotificationEnabled) {
                                LiquidButton(
                                    onClick = {
                                        OverlayPermissionHelper.launchOverlaySettings(context)
                                    },
                                    backdrop = backdrop,
                                    surfaceColor = accentColor.copy(alpha = 0.85f),
                                ) {
                                    LiquidText(
                                        text = "Permission",
                                        style = AppTypography.Caption,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                            }

                            LiquidButton(
                                onClick = {
                                    val newState = !settings.notchNotificationEnabled
                                    settings.setNotchNotification(newState)
                                    if (newState && !canOverlay) {
                                        OverlayPermissionHelper.launchOverlaySettings(context)
                                    }
                                },
                                backdrop = backdrop,
                                surfaceColor = if (settings.notchNotificationEnabled && canOverlay) {
                                    Color(0xFF34C759).copy(alpha = 0.85f)
                                } else surfaceColor,
                            ) {
                                LiquidText(
                                    text = if (settings.notchNotificationEnabled) "On" else "Off",
                                    style = AppTypography.Caption,
                                    fontWeight = FontWeight.Bold,
                                    color = if (settings.notchNotificationEnabled && canOverlay) Color.White else secondaryTextColor,
                                )
                            }
                        }
                    }
                }

                // Background Image Setting
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidText(
                        text = "Wallpaper & Luminance",
                        style = AppTypography.Headline,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )
                    LiquidText(
                        text = if (settings.backgroundUri.isEmpty()) {
                            "Default fluid gradient · Luminance adaptive"
                        } else {
                            "Custom wallpaper · Colors adapt automatically"
                        },
                        style = AppTypography.Footnote,
                        color = secondaryTextColor,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LiquidButton(
                            onClick = { pickImage.launch(arrayOf("image/*")) },
                            backdrop = backdrop,
                            modifier = Modifier.weight(1f),
                            surfaceColor = surfaceColor,
                        ) {
                            LiquidText(
                                text = "Choose Image",
                                style = AppTypography.Callout,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                            )
                        }

                        if (settings.backgroundUri.isNotEmpty()) {
                            LiquidButton(
                                onClick = { settings.setBackground("") },
                                backdrop = backdrop,
                                surfaceColor = surfaceColor,
                            ) {
                                LiquidText(
                                    text = "Reset",
                                    style = AppTypography.Callout,
                                    color = secondaryTextColor,
                                )
                            }
                        }
                    }
                }

                // Ringtone Setting
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidText(
                        text = "Alarm Sound",
                        style = AppTypography.Headline,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )
                    LiquidText(
                        text = if (settings.ringtoneUri.isEmpty()) "Built-in bell chime" else "Custom audio file",
                        style = AppTypography.Footnote,
                        color = secondaryTextColor,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LiquidButton(
                            onClick = { pickSound.launch(arrayOf("audio/*")) },
                            backdrop = backdrop,
                            modifier = Modifier.weight(1f),
                            surfaceColor = surfaceColor,
                        ) {
                            LiquidText(
                                text = "Choose Sound",
                                style = AppTypography.Callout,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                            )
                        }

                        if (settings.ringtoneUri.isNotEmpty()) {
                            LiquidButton(
                                onClick = { settings.setRingtone("") },
                                backdrop = backdrop,
                                surfaceColor = surfaceColor,
                            ) {
                                LiquidText(
                                    text = "Reset",
                                    style = AppTypography.Callout,
                                    color = secondaryTextColor,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Done Button
                LiquidButton(
                    onClick = onDismiss,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth(),
                    tint = accentColor,
                    surfaceColor = accentColor.copy(alpha = 0.85f),
                ) {
                    LiquidText(
                        text = "Done",
                        style = AppTypography.Headline,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
