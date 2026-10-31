package com.rifsxd.ksunext.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

private val glassBorder = BorderStroke(
    1.dp,
    Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.60f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.30f))
    )
)

@Composable
private fun GlassCard(
    modifier: Modifier,
    shape: Shape,
    colors: CardColors,
    border: BorderStroke?,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = colors.containerColor
    // bright (status) cards stay glossy-solid, dark cards become see-through glass
    val fill = base.copy(alpha = if (base.luminance() > 0.28f) 0.82f else 0.38f)
    androidx.compose.material3.Card(
        modifier = modifier
            .clip(shape)
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                        endY = size.height * 0.55f
                    )
                )
            },
        shape = shape,
        colors = colors.copy(containerColor = fill),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = border ?: glassBorder,
        content = content
    )
}

@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    content: @Composable ColumnScope.() -> Unit
) = GlassCard(modifier, shape, colors, null, content)

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) = GlassCard(modifier, shape, colors, border, content)

@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.outlinedShape,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) = GlassCard(modifier, shape, colors, border, content)
