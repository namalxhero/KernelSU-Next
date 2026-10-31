package com.rifsxd.ksunext.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rifsxd.ksunext.R
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

val RootXCyan = Color(0xFF3CC8FF)
val RootXIce = Color(0xFFBFF0FF)

/** Frosted glass surface: translucent gradient fill and a soft light border. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)): Modifier = this
    .clip(shape)
    .background(
        Brush.linearGradient(listOf(Color(0x24FFFFFF), Color(0x0AFFFFFF))),
        shape
    )
    .border(
        1.dp,
        Brush.linearGradient(listOf(Color(0x55FFFFFF), Color(0x14FFFFFF))),
        shape
    )

enum class XBoomMood { Open, Happy, Scared, Sleepy }

/** The xBooM mascot. Expression is a separate layer so it can change (and blink). */
@Composable
fun XBoom(
    modifier: Modifier = Modifier,
    mood: XBoomMood = XBoomMood.Open,
    blink: Boolean = true,
    bob: Boolean = false
) {
    var blinking by remember { mutableStateOf(false) }
    LaunchedEffect(blink, mood) {
        if (blink && mood == XBoomMood.Open) {
            while (true) {
                delay(2800)
                blinking = true
                delay(130)
                blinking = false
            }
        } else {
            blinking = false
        }
    }
    val transition = rememberInfiniteTransition(label = "xboom")
    val bobValue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val shown = if (blinking) XBoomMood.Sleepy else mood
    val eyes = when (shown) {
        XBoomMood.Open -> R.drawable.xboom_eyes_open
        XBoomMood.Happy -> R.drawable.xboom_eyes_happy
        XBoomMood.Scared -> R.drawable.xboom_eyes_scared
        XBoomMood.Sleepy -> R.drawable.xboom_eyes_sleepy
    }
    Box(
        modifier
            .aspectRatio(120f / 150f)
            .graphicsLayer { translationY = if (bob) (bobValue * 6f - 3f) * density else 0f }
    ) {
        Image(painterResource(R.drawable.xboom_base), contentDescription = null, modifier = Modifier.fillMaxSize())
        Image(painterResource(eyes), contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}

/** Loading indicator: xBooM bobbing in the middle of three orbiting dots. */
@Composable
fun XLoader(modifier: Modifier = Modifier, label: String = "Loading") {
    val transition = rememberInfiniteTransition(label = "xloader")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2600, easing = LinearEasing)),
        label = "angle"
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f - 8.dp.toPx()
                drawCircle(
                    color = Color(0x336FDCFF),
                    radius = r,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 18f))
                    )
                )
                val dots = listOf(
                    Color(0xFF6FDCFF) to 0f,
                    Color(0xFFC3A4FF) to 120f,
                    Color(0xFFFF8FC4) to 240f
                )
                dots.forEach { (c, off) ->
                    val a = Math.toRadians((angle + off).toDouble())
                    drawCircle(
                        color = c,
                        radius = 5.dp.toPx(),
                        center = Offset(center.x + (cos(a) * r).toFloat(), center.y + (sin(a) * r).toFloat())
                    )
                }
            }
            XBoom(Modifier.height(104.dp), mood = XBoomMood.Open, bob = true)
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color(0xFFBFE9F7), fontSize = 13.sp)
    }
}

/** Black backdrop with soft colored glows, so glass surfaces have something to refract. */
@Composable
fun RootXBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(Color.Black)
        val w = size.width
        val h = size.height
        fun glow(c: Color, cx: Float, cy: Float, r: Float) {
            drawCircle(
                brush = Brush.radialGradient(listOf(c, Color.Transparent), center = Offset(cx, cy), radius = r),
                radius = r,
                center = Offset(cx, cy)
            )
        }
        glow(Color(0x661AA7E8), w * 0.95f, h * 0.10f, w * 0.75f)
        glow(Color(0x557A4DFF), w * 0.0f, h * 0.45f, w * 0.7f)
        glow(Color(0x33FF5FA8), w * 0.9f, h * 0.80f, w * 0.6f)
    }
}

/** Splash shown on launch: xBooM in a glowing portal, the RootX name and a progress line. */
@Composable
fun RootXSplashOverlay() {
    var show by remember { mutableStateOf(true) }
    var target by remember { mutableStateOf(0f) }
    val progress by animateFloatAsState(target, tween(1300, easing = FastOutSlowInEasing), label = "splash")
    LaunchedEffect(Unit) {
        target = 1f
        delay(1500)
        show = false
    }
    AnimatedVisibility(visible = show, enter = EnterTransition.None, exit = fadeOut(tween(350))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            RootXBackdrop()
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(Color(0x143CC8FF))
                        .border(2.dp, Color(0xFF6FDCFF), CircleShape),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    XBoom(Modifier.height(190.dp), mood = XBoomMood.Happy, blink = false, bob = true)
                }
                Spacer(Modifier.height(28.dp))
                Text("RootX", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Root, your way", color = Color(0xFF9FC4D2), fontSize = 13.sp)
                Spacer(Modifier.height(34.dp))
                Box(
                    Modifier
                        .width(150.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x1FFFFFFF))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(Brush.horizontalGradient(listOf(RootXCyan, Color(0xFFC3A4FF))))
                    )
                }
            }
        }
    }
}
