package com.crischarles.bitlog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Egg Recipes Screen - A dedicated screen showing recipes for different egg types
 * with Kyant Liquid Glass UI and physics-based button interactions.
 */
@Composable
fun EggRecipesScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val backdrop = rememberLayerBackdrop {
        drawRect(Color(0xFFEEF2FA))
        drawContent()
    }

    var selectedEggType by remember { mutableStateOf(EggType.CHICKEN) }
    var showRecipeDetail by remember { mutableStateOf<EggPreset?>(null) }
    var presetsOpen by remember { mutableStateOf(true) }

    // Background photo plus its average luminance, loaded off the main thread.
    val background by produceState<Pair<ImageBitmap?, Float>>(
        initialValue = null to DefaultLuminance,
        settings.backgroundUri,
    ) {
        value = if (settings.backgroundUri.isEmpty()) {
            null to DefaultLuminance
        } else {
            withContext(Dispatchers.IO) {
                loadEggRecipeBackground(context, settings.backgroundUri)
            }
        }
    }
    val photo = background.first
    val imageLuminance = background.second

    val animatedLuminance = animateFloatAsState(
        targetValue = imageLuminance,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "animated_luminance",
    )

    val isLightBackground = animatedLuminance.value > 0.48f
    val textColor = if (isLightBackground) Color(0xFF0F172A) else Color(0xFFF9FAFB)
    val secondaryTextColor = if (isLightBackground) Color(0xFF475569) else Color(0xCCF9FAFB)
    val glassSurfaceColor = if (isLightBackground) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f)
    val accentColor = Color(0xFFFF9500)

    Box(Modifier.fillMaxSize()) {
        EggRecipeBackground(photo = photo, backdrop = backdrop)

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Navigation bar with back button
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LiquidIconButton(
                    onClick = onBack,
                    backdrop = backdrop,
                    size = 46.dp,
                    surfaceColor = glassSurfaceColor,
                ) {
                    LiquidText(
                        text = "←",
                        style = AppTypography.Title2,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                }

                Column {
                    LiquidText(
                        text = "Egg Recipes",
                        style = AppTypography.LargeTitle,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                    LiquidText(
                        text = "${AllEggTypes.size} egg types · ${EggPresets.size} cooking styles",
                        style = AppTypography.Caption,
                        color = secondaryTextColor,
                    )
                }

                Spacer(modifier = Modifier.width(46.dp))
            }

            // Egg type selector
            LiquidGlassPanel(
                backdrop = backdrop,
                surfaceColor = glassSurfaceColor,
            ) {
                LiquidText(
                    text = "Select Egg Type",
                    style = AppTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                )

                PhysicsEggTypeSelector(
                    selectedType = selectedEggType,
                    onTypeSelected = { selectedEggType = it },
                    backdrop = backdrop,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    glassSurfaceColor = glassSurfaceColor,
                    accentColor = accentColor,
                )

                LiquidText(
                    text = selectedEggType.description,
                    style = AppTypography.Caption,
                    color = secondaryTextColor,
                )
            }

            // Cooking styles for the selected egg type
            AnimatedVisibility(
                visible = showRecipeDetail == null,
                enter = fadeIn(spring(dampingRatio = 0.78f, stiffness = 380f)),
                exit = fadeOut(spring(dampingRatio = 0.78f, stiffness = 380f)),
            ) {
                LiquidGlassPanel(
                    backdrop = backdrop,
                    surfaceColor = glassSurfaceColor,
                ) {
                    AccordionSection(
                        title = "${selectedEggType.displayName} Egg Cooking Styles",
                        expanded = presetsOpen,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        onToggle = { presetsOpen = !presetsOpen },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EggPresets.forEach { preset ->
                                val cookTime = preset.getCookTimeFor(selectedEggType)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(glassSurfaceColor)
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        LiquidText(
                                            text = preset.name,
                                            style = AppTypography.Headline,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor,
                                        )
                                        LiquidText(
                                            text = preset.description.ifEmpty { "Cooked to perfection" },
                                            style = AppTypography.Footnote,
                                            color = secondaryTextColor,
                                        )
                                    }

                                    PhysicsButton(
                                        text = formatTime(cookTime),
                                        onClick = { showRecipeDetail = preset },
                                        backdrop = backdrop,
                                        surfaceColor = accentColor.copy(alpha = 0.35f),
                                        textColor = accentColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recipe detail for the chosen cooking style
            showRecipeDetail?.let { preset ->
                val cookTime = preset.getCookTimeFor(selectedEggType)
                RecipeDetailCard(
                    preset = preset,
                    eggType = selectedEggType,
                    cookTime = cookTime,
                    backdrop = backdrop,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    glassSurfaceColor = glassSurfaceColor,
                    accentColor = accentColor,
                    onClose = { showRecipeDetail = null },
                    onStartTimer = {
                        EggTimerManager.startTimer(
                            context = context,
                            label = preset.getDisplayName(selectedEggType),
                            seconds = cookTime,
                        )
                        showRecipeDetail = null
                    },
                )
            }

            // All egg types
            LiquidGlassPanel(
                backdrop = backdrop,
                surfaceColor = glassSurfaceColor,
            ) {
                LiquidText(
                    text = "All Egg Types",
                    style = AppTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AllEggTypes.forEach { eggType ->
                        EggTypeInfoRow(
                            eggType = eggType,
                            isSelected = eggType == selectedEggType,
                            backdrop = backdrop,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            glassSurfaceColor = glassSurfaceColor,
                            accentColor = accentColor,
                            onClick = { selectedEggType = eggType },
                        )
                    }
                }
            }
        }
    }
}

private const val DefaultLuminance = 0.82f

/** Decodes the custom background and returns it with its average luminance. Runs on IO. */
private fun loadEggRecipeBackground(context: Context, uriString: String): Pair<ImageBitmap?, Float> {
    return try {
        context.contentResolver.openInputStream(Uri.parse(uriString))?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream) ?: return null to DefaultLuminance
            val sample = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
            val pixels = IntArray(256)
            sample.getPixels(pixels, 0, 16, 0, 0, 16, 16)
            var sum = 0.0
            for (p in pixels) {
                val r = (p shr 16 and 0xFF) / 255.0
                val g = (p shr 8 and 0xFF) / 255.0
                val b = (p and 0xFF) / 255.0
                sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
            }
            bitmap.asImageBitmap() to (sum / pixels.size).toFloat()
        } ?: (null to DefaultLuminance)
    } catch (e: Exception) {
        null to DefaultLuminance
    }
}

/**
 * Egg type selector: a horizontally scrolling row of draggable glass buttons.
 */
@Composable
fun PhysicsEggTypeSelector(
    selectedType: EggType,
    onTypeSelected: (EggType) -> Unit,
    backdrop: LayerBackdrop,
    textColor: Color,
    secondaryTextColor: Color,
    glassSurfaceColor: Color,
    accentColor: Color,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AllEggTypes.forEach { eggType ->
            PhysicsDragButton(
                eggType = eggType,
                isSelected = eggType == selectedType,
                onClick = { onTypeSelected(eggType) },
                backdrop = backdrop,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                glassSurfaceColor = glassSurfaceColor,
                accentColor = accentColor,
            )
        }
    }
}

/**
 * Glass button that springs to a press scale when held, follows the finger horizontally
 * while dragged, and springs back on release. A release with little movement counts as a tap.
 */
@Composable
fun PhysicsDragButton(
    eggType: EggType,
    isSelected: Boolean,
    onClick: () -> Unit,
    backdrop: LayerBackdrop,
    textColor: Color,
    secondaryTextColor: Color,
    glassSurfaceColor: Color,
    accentColor: Color,
) {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val pressScale = remember { Animatable(1f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    val springSpec = spring<Float>(dampingRatio = 0.65f, stiffness = 450f)
    val tapSlopPx = with(LocalDensity.current) { 8.dp.toPx() }
    val shape = RoundedCornerShape(24.dp)

    val surface = if (supportsLiquidGlass) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(8f.dp.toPx())
                lens(12f.dp.toPx(), 24f.dp.toPx())
            },
            highlight = { Highlight.Default },
            onDrawSurface = {
                val bgColor = if (isSelected) accentColor.copy(alpha = 0.45f) else glassSurfaceColor
                drawRect(bgColor)
                if (isPressed) drawRect(Color.White.copy(alpha = 0.15f))
            },
        )
    } else {
        Modifier
            .clip(shape)
            .background(if (isSelected) accentColor.copy(alpha = 0.45f) else glassSurfaceColor.copy(alpha = 0.65f))
    }

    Box(
        modifier = Modifier
            .size(80.dp, 100.dp)
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
                translationX = dragOffset
            }
            .then(surface)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isPressed = true
                    scope.launch { pressScale.animateTo(1.08f, springSpec) }

                    var totalMove = 0f
                    var lifted = false
                    while (!lifted) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) {
                            lifted = true
                        } else {
                            val dx = change.position.x - change.previousPosition.x
                            if (dx != 0f) {
                                change.consume()
                                totalMove += abs(dx)
                                dragOffset += dx
                            }
                        }
                    }

                    isPressed = false
                    scope.launch {
                        pressScale.animateTo(1f, springSpec)
                    }
                    scope.launch {
                        animate(dragOffset, 0f, animationSpec = springSpec) { value, _ -> dragOffset = value }
                    }
                    if (lifted && totalMove < tapSlopPx) currentOnClick()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EggTypeIcon(
                eggType = eggType,
                iconSize = 40.dp,
                color = if (isSelected) accentColor else textColor,
            )

            LiquidText(
                text = eggType.displayName,
                style = AppTypography.Callout,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else textColor,
            )

            LiquidText(
                text = "${(eggType.sizeMultiplier * 100).toInt()}% size",
                style = AppTypography.Caption,
                color = secondaryTextColor,
            )
        }
    }
}

/**
 * Simple egg icon drawn on a canvas, tinted by egg type.
 */
@Composable
fun EggTypeIcon(
    eggType: EggType,
    iconSize: Dp,
    color: Color,
) {
    Canvas(modifier = Modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height
        val scale = eggType.sizeMultiplier.coerceIn(0.15f, 1.0f)

        val eggColor = when (eggType) {
            EggType.QUAIL -> Color(0xFFDDB892)
            EggType.BANTAM -> Color(0xFFF5E6D3)
            EggType.CHICKEN -> Color(0xFFFFF8E7)
            EggType.DUCK -> Color(0xFFE8F5E9)
            EggType.GOOSE -> Color(0xFFFFFFFF)
            EggType.TURKEY -> Color(0xFFFBE9E7)
            EggType.OSTRICH -> Color(0xFFFFFFFF)
            EggType.EMU -> Color(0xFF7B8D6B)
        }

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    eggColor.copy(alpha = 0.9f),
                    eggColor.copy(alpha = 0.6f),
                    color.copy(alpha = 0.3f),
                ),
                center = Offset(-w * 0.15f * scale, -h * 0.2f * scale),
                radius = w * 0.7f * scale,
            ),
            topLeft = Offset(w * 0.5f - w * 0.45f * scale, h * 0.5f - h * 0.55f * scale),
            size = Size(w * 0.9f * scale, h * 1.1f * scale),
        )
    }
}

/**
 * Recipe detail card with full cooking instructions.
 */
@Composable
fun RecipeDetailCard(
    preset: EggPreset,
    eggType: EggType,
    cookTime: Int,
    backdrop: LayerBackdrop,
    textColor: Color,
    secondaryTextColor: Color,
    glassSurfaceColor: Color,
    accentColor: Color,
    onClose: () -> Unit,
    onStartTimer: () -> Unit,
) {
    val details = getRecipeDetails(preset, eggType)

    LiquidGlassPanel(
        backdrop = backdrop,
        surfaceColor = glassSurfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(IosIntSizeSpring),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                LiquidText(
                    text = "${eggType.displayName} ${preset.name}",
                    style = AppTypography.Title2,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
                LiquidText(
                    text = "Cook time: ${formatTime(cookTime)}",
                    style = AppTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                )
            }

            LiquidIconButton(
                onClick = onClose,
                backdrop = backdrop,
                size = 40.dp,
                surfaceColor = glassSurfaceColor,
            ) {
                LiquidText(
                    text = "✕",
                    style = AppTypography.Title2,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            details.forEach { detail ->
                RecipeDetailRow(
                    icon = detail.icon,
                    title = detail.title,
                    description = detail.description,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                )
            }

            PhysicsButton(
                text = "Start Timer",
                onClick = onStartTimer,
                backdrop = backdrop,
                surfaceColor = accentColor.copy(alpha = 0.9f),
                textColor = Color.White,
                icon = "▶",
            )
        }
    }
}

data class RecipeDetail(
    val icon: String,
    val title: String,
    val description: String,
)

fun getRecipeDetails(preset: EggPreset, eggType: EggType): List<RecipeDetail> {
    val baseDetails = when (preset.name) {
        "Poached" -> listOf(
            RecipeDetail("🌊", "Water Temperature", "Simmering (not boiling) at 85-90°C / 185-195°F"),
            RecipeDetail("🥚", "Egg Prep", "Use fresh eggs, crack into small bowl first"),
            RecipeDetail("🥄", "Technique", "Create gentle whirlpool, slide egg in center"),
            RecipeDetail("⏱", "Timing", "Don't overcook - residual heat continues cooking"),
        )
        "Soft-boiled" -> listOf(
            RecipeDetail("💧", "Water Start", "Start with boiling water, then reduce to simmer"),
            RecipeDetail("🧊", "Ice Bath", "Immediately transfer to ice water for 1 min"),
            RecipeDetail("🥚", "Egg Temp", "Room temperature eggs prevent cracking"),
            RecipeDetail("🔪", "Peeling", "Tap gently, peel under running water"),
        )
        "Jammy / Ramen" -> listOf(
            RecipeDetail("🍜", "Ramen Style", "Marinate in soy sauce, mirin, sake overnight"),
            RecipeDetail("🧊", "Shock Cool", "Ice bath critical for jammy texture"),
            RecipeDetail("🥢", "Serving", "Slice in half, place on ramen"),
            RecipeDetail("🌿", "Garnish", "Green onions, nori, sesame seeds"),
        )
        "Medium" -> listOf(
            RecipeDetail("🥚", "Perfect For", "Egg salad, deviled eggs, snacking"),
            RecipeDetail("🧊", "Cool Down", "Ice bath 5 minutes for easy peel"),
            RecipeDetail("🔪", "Slicing", "Use wet knife for clean cuts"),
        )
        "Hard-boiled" -> listOf(
            RecipeDetail("🥗", "Meal Prep", "Store unpeeled up to 1 week in fridge"),
            RecipeDetail("🧊", "Green Ring", "Rapid cooling prevents gray-green yolk ring"),
            RecipeDetail("🥄", "Yolk Uses", "Mash for deviled eggs, egg salad, garnish"),
        )
        "Onsen Tamago" -> listOf(
            RecipeDetail("♨️", "Method", "Slow cook at 63-65°C / 145-149°F"),
            RecipeDetail("🍚", "Serving", "In dashi broth with soy sauce, green onions"),
            RecipeDetail("⏳", "Patience", "Low temp = longer time, worth the wait"),
        )
        else -> listOf(
            RecipeDetail("🥚", "General", "Use timer for consistent results"),
            RecipeDetail("🧊", "Always", "Ice bath stops cooking instantly"),
        )
    }

    val typeTips = when (eggType) {
        EggType.QUAIL -> listOf(
            RecipeDetail("🐣", "Quail Specific", "Cook 1/5th the time of chicken eggs"),
            RecipeDetail("🔍", "Peeling", "Roll gently on counter, peel from wide end"),
        )
        EggType.DUCK -> listOf(
            RecipeDetail("🦆", "Duck Specific", "Richer yolk, whites set faster"),
            RecipeDetail("⏱", "Adjustment", "Add 30-60 seconds vs chicken eggs"),
        )
        EggType.GOOSE -> listOf(
            RecipeDetail("🪿", "Goose Specific", "Huge eggs - may need 2-3x chicken time"),
            RecipeDetail("🍳", "Best For", "Baking, rich custards, feeding crowds"),
        )
        EggType.OSTRICH -> listOf(
            RecipeDetail("🦩", "Ostrich Specific", "One egg = ~24 chicken eggs!"),
            RecipeDetail("🔪", "Cracking", "Need hammer or drill to crack shell"),
            RecipeDetail("🍳", "Cooking", "Scramble or make massive omelette"),
        )
        EggType.EMU -> listOf(
            RecipeDetail("🦘", "Emu Specific", "Dark green shell, ~12 chicken eggs"),
            RecipeDetail("🍳", "Flavor", "Richer, gamier than chicken"),
        )
        else -> emptyList()
    }

    return baseDetails + typeTips
}

@Composable
fun RecipeDetailRow(
    icon: String,
    title: String,
    description: String,
    textColor: Color,
    secondaryTextColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LiquidText(
            text = icon,
            style = AppTypography.Title2,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            LiquidText(
                text = title,
                style = AppTypography.Callout,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
            )
            LiquidText(
                text = description,
                style = AppTypography.Footnote,
                color = secondaryTextColor,
            )
        }
    }
}

/**
 * Button with a spring press animation (scale), used for cook times and actions.
 */
@Composable
fun PhysicsButton(
    text: String,
    onClick: () -> Unit,
    backdrop: LayerBackdrop,
    surfaceColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    icon: String = "",
) {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val pressScale = remember { Animatable(1f) }
    val springSpec = spring<Float>(dampingRatio = 0.55f, stiffness = 500f)
    val shape = RoundedCornerShape(18.dp)

    val surface = if (supportsLiquidGlass) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(6f.dp.toPx())
                lens(10f.dp.toPx(), 20f.dp.toPx())
            },
            highlight = { Highlight.Default },
            onDrawSurface = { drawRect(surfaceColor) },
        )
    } else {
        Modifier.clip(shape).background(surfaceColor.copy(alpha = 0.8f))
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .then(surface)
            .pressSpring(scope, pressScale, springSpec, pressedScale = 0.92f) { currentOnClick() }
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon.isNotBlank()) {
            LiquidText(
                text = icon,
                style = AppTypography.Title2,
                color = textColor,
            )
        }
        LiquidText(
            text = text,
            style = AppTypography.Headline,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

/**
 * Egg type row for the list view.
 */
@Composable
fun EggTypeInfoRow(
    eggType: EggType,
    isSelected: Boolean,
    onClick: () -> Unit,
    backdrop: LayerBackdrop,
    textColor: Color,
    secondaryTextColor: Color,
    glassSurfaceColor: Color,
    accentColor: Color,
) {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val pressScale = remember { Animatable(1f) }
    val springSpec = spring<Float>(dampingRatio = 0.65f, stiffness = 450f)
    val shape = RoundedCornerShape(16.dp)
    val surface = if (isSelected) accentColor.copy(alpha = 0.35f) else glassSurfaceColor

    val surfaceModifier = if (supportsLiquidGlass) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(6f.dp.toPx())
                lens(8f.dp.toPx(), 16f.dp.toPx())
            },
            highlight = { Highlight.Default },
            onDrawSurface = { drawRect(surface) },
        )
    } else {
        Modifier.clip(shape).background(surface.copy(alpha = 0.65f))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .then(surfaceModifier)
            .pressSpring(scope, pressScale, springSpec, pressedScale = 0.96f) { currentOnClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            EggTypeIcon(eggType = eggType, iconSize = 36.dp, color = if (isSelected) accentColor else textColor)

            Column {
                LiquidText(
                    text = eggType.displayName,
                    style = AppTypography.Headline,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) accentColor else textColor,
                )
                LiquidText(
                    text = eggType.description,
                    style = AppTypography.Caption,
                    color = secondaryTextColor,
                )
            }
        }

        LiquidText(
            text = "${(eggType.sizeMultiplier * 100).toInt()}% size",
            style = AppTypography.Caption,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) accentColor else secondaryTextColor,
        )
    }
}

/**
 * Press feedback shared by the buttons: springs to [pressedScale] on touch down and back on release.
 */
private fun Modifier.pressSpring(
    scope: CoroutineScope,
    scale: Animatable<Float, AnimationVector1D>,
    spec: SpringSpec<Float>,
    pressedScale: Float,
    onTap: () -> Unit,
): Modifier = pointerInput(Unit) {
    detectTapGestures(
        onPress = {
            scope.launch { scale.animateTo(pressedScale, spec) }
            tryAwaitRelease()
            scope.launch { scale.animateTo(1f, spec) }
        },
        onTap = { onTap() },
    )
}

/**
 * Background layer (shared look with EggTimerScreen).
 */
@Composable
private fun EggRecipeBackground(photo: ImageBitmap?, backdrop: LayerBackdrop) {
    Box(
        Modifier
            .fillMaxSize()
            .layerBackdrop(backdrop),
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFEEF2FA), Color(0xFFDDE7F5), Color(0xFFCAD8EC)),
                        ),
                    ),
            )
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Color(0xFFFFB48A).copy(alpha = 0.55f),
                    radius = size.minDimension * 0.38f,
                    center = Offset(size.width * 0.1f, size.height * 0.15f),
                )
                drawCircle(
                    Color(0xFF8AD7FF).copy(alpha = 0.50f),
                    radius = size.minDimension * 0.44f,
                    center = Offset(size.width * 0.95f, size.height * 0.55f),
                )
                drawCircle(
                    Color(0xFFB9F5A8).copy(alpha = 0.45f),
                    radius = size.minDimension * 0.32f,
                    center = Offset(size.width * 0.25f, size.height * 0.9f),
                )
                drawCircle(
                    Color(0xFFD7B8FF).copy(alpha = 0.40f),
                    radius = size.minDimension * 0.28f,
                    center = Offset(size.width * 0.8f, size.height * 0.05f),
                )
            }
        }
    }
}
