package com.crischarles.bitlog

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/** Android 13+ gets the full lens effect; Android 12 gets blur and vibrancy; older versions get a tinted panel. */
private val supportsGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * A Liquid Glass panel. On Android 12+ it refracts the backdrop layer behind it.
 * On older versions it draws a translucent tinted panel with no blur.
 */
@Composable
fun GlassPanel(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    corner: androidx.compose.ui.unit.Dp = 28.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shapeRadius = corner
    val base = if (supportsGlass) {
        modifier.fillMaxWidth().drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedCornerShape(shapeRadius) },
            effects = {
                vibrancy()
                blur(8f.dp.toPx())
                lens(12f.dp.toPx(), 24f.dp.toPx())
            },
            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) },
        )
    } else {
        modifier.fillMaxWidth()
            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(shapeRadius))
    }
    Column(
        base.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
