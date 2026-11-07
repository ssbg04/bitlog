package com.crischarles.bitlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass Accordion section with Apple iOS spring-driven expand/collapse animation.
 * Free of Material UI dependencies.
 */
@Composable
fun AccordionSection(
    title: String,
    expanded: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = IosInteractiveSpring,
        label = "accordion_chevron",
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle,
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LiquidText(
                text = title,
                style = AppTypography.Title2,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.weight(1f),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LiquidText(
                    text = if (expanded) "Hide" else "Show",
                    style = AppTypography.Footnote,
                    color = secondaryTextColor,
                )
                Box(modifier = Modifier.rotate(chevronRotation)) {
                    LiquidText(
                        text = "▼",
                        style = AppTypography.Caption,
                        color = secondaryTextColor,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(IosInteractiveSpring) + expandVertically(IosInteractiveSpring),
            exit = fadeOut(IosInteractiveSpring) + shrinkVertically(IosInteractiveSpring),
        ) {
            content()
        }
    }
}
