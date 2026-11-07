package com.crischarles.bitlog

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

/**
 * Apple iOS Liquid Glass Settings Modal Dialog.
 * Replaces Material UI AlertDialog with a pure Kyant0 backdrop glass sheet.
 */
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Scrim overlay with tap outside to dismiss
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
            // Glass modal card
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
                verticalArrangement = Arrangement.spacedBy(20.dp),
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

                Spacer(modifier = Modifier.height(4.dp))

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
