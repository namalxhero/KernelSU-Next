package com.rifsxd.ksunext.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val TWO_PI = (2.0 * PI).toFloat()

private class NovaPalette(
    val sky: List<Color>,
    val moon: Color,
    val glow1: Color,
    val glow2: Color,
    val glow3: Color,
    val star: Color,
    val particle: Color,
    val city: Color,
    val window: Color,
)

private class NovaStar(val x: Float, val y: Float, val r: Float, val k: Int, val phase: Float)
private class NovaDrifter(val x: Float, val size: Float, val cycles: Int, val phase: Float, val sway: Float, val swayK: Int, val rot: Int)
private class NovaBuilding(val x: Float, val w: Float, val h: Float, val i: Int)

private fun novaPalette(theme: Int): NovaPalette = when (theme) {
    NOVA_OCEAN_GLASS -> NovaPalette(
        sky = listOf(Color(0xFF030B16), Color(0xFF06203A), Color(0xFF0A4A6B), Color(0xFF1291A8)),
        moon = Color(0xFFD6F6FF),
        glow1 = Color(0x554DD0E1), glow2 = Color(0x445C9DFF), glow3 = Color(0x559AE6FF),
        star = Color(0xFFE8FAFF), particle = Color(0xFFBFF3FF),
        city = Color(0xFF03101C), window = Color(0xFF7DE8FF),
    )
    NOVA_MIDNIGHT_AMOLED -> NovaPalette(
        sky = listOf(Color(0xFF000000), Color(0xFF020A06), Color(0xFF04160D), Color(0xFF06301B)),
        moon = Color(0xFFB8FFD6),
        glow1 = Color(0x4439FF88), glow2 = Color(0x3300E676), glow3 = Color(0x44B2FF59),
        star = Color(0xFFCFFFE5), particle = Color(0xFF39FF88),
        city = Color(0xFF000000), window = Color(0xFF39FF88),
    )
    else -> NovaPalette(
        sky = listOf(Color(0xFF070419), Color(0xFF1B0F47), Color(0xFF4A1A78), Color(0xFF9B3A8C)),
        moon = Color(0xFFFFE9F6),
        glow1 = Color(0x55B388FF), glow2 = Color(0x44FF7EB6), glow3 = Color(0x557DE8FF),
        star = Color(0xFFFFFFFF), particle = Color(0xFFFFB7D5),
        city = Color(0xFF0A0616), window = Color(0xFFFFD98A),
    )
}

/** Live anime style background: starry sky, moon, shooting stars, falling petals / bubbles, city lights. */
@Composable
fun NovaBackground(novaTheme: Int, live: Boolean, modifier: Modifier = Modifier) {
    val pal = remember(novaTheme) { novaPalette(novaTheme) }
    val stars = remember {
        List(90) { i ->
            val r = Random(100 + i)
            NovaStar(r.nextFloat(), r.nextFloat() * 0.6f, 0.6f + r.nextFloat() * 1.5f, 6 + r.nextInt(30), r.nextFloat())
        }
    }
    val drifters = remember {
        List(26) { i ->
            val r = Random(500 + i)
            NovaDrifter(r.nextFloat(), 5f + r.nextFloat() * 9f, 2 + r.nextInt(4), r.nextFloat(), 10f + r.nextFloat() * 28f, 3 + r.nextInt(5), 1 + r.nextInt(3))
        }
    }
    val buildings = remember {
        val list = ArrayList<NovaBuilding>()
        var x = -0.02f
        var i = 0
        while (x < 1.02f) {
            val r = Random(900 + i)
            val w = 0.035f + r.nextFloat() * 0.06f
            list.add(NovaBuilding(x, w, 0.03f + r.nextFloat() * 0.09f, i))
            x += w + 0.004f
            i++
        }
        list
    }

    if (live) {
        val transition = rememberInfiniteTransition(label = "novaBg")
        val t = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart),
            label = "novaT"
        )
        NovaCanvas(modifier, pal, novaTheme, stars, drifters, buildings) { t.value }
    } else {
        NovaCanvas(modifier, pal, novaTheme, stars, drifters, buildings) { 0.35f }
    }
}

private fun DrawScope.glow(cx: Float, cy: Float, r: Float, c: Color) {
    drawCircle(
        Brush.radialGradient(listOf(c, Color.Transparent), center = Offset(cx, cy), radius = r),
        radius = r,
        center = Offset(cx, cy)
    )
}

@Composable
private fun NovaCanvas(
    modifier: Modifier,
    pal: NovaPalette,
    theme: Int,
    stars: List<NovaStar>,
    drifters: List<NovaDrifter>,
    buildings: List<NovaBuilding>,
    time: () -> Float,
) {
    Canvas(modifier.fillMaxSize()) {
        val t = time()
        val w = size.width
        val h = size.height
        val d = density
        val a = TWO_PI * t

        // sky
        drawRect(Brush.verticalGradient(pal.sky))

        // drifting aurora glows
        glow(w * (0.22f + 0.12f * sin(a)), h * (0.24f + 0.05f * cos(a)), w * 0.95f, pal.glow1)
        glow(w * (0.82f - 0.10f * cos(a)), h * (0.52f + 0.06f * sin(a)), w * 0.85f, pal.glow2)
        glow(w * (0.45f + 0.18f * sin(2f * a)), h * 0.92f, w * 0.95f, pal.glow3)

        // moon
        val mx = w * 0.76f
        val my = h * 0.14f + sin(a) * h * 0.004f
        val mr = w * 0.085f
        glow(mx, my, mr * 4.2f, pal.moon.copy(alpha = 0.30f))
        drawCircle(
            Brush.linearGradient(listOf(Color.White, pal.moon), start = Offset(mx - mr, my - mr), end = Offset(mx + mr, my + mr)),
            radius = mr,
            center = Offset(mx, my)
        )

        // twinkling stars
        for (s in stars) {
            val tw = 0.25f + 0.75f * abs(sin(TWO_PI * (t * s.k + s.phase)))
            val c = Offset(s.x * w, s.y * h)
            drawCircle(pal.star.copy(alpha = tw), s.r * d, c)
            if (s.r > 1.7f) {
                val l = s.r * d * 3.2f
                drawLine(pal.star.copy(alpha = tw * 0.7f), Offset(c.x - l, c.y), Offset(c.x + l, c.y), strokeWidth = d * 0.8f)
                drawLine(pal.star.copy(alpha = tw * 0.7f), Offset(c.x, c.y - l), Offset(c.x, c.y + l), strokeWidth = d * 0.8f)
            }
        }

        // shooting stars
        for (n in 0..1) {
            val s = (t * (5 + 2 * n) + n * 0.37f) % 1f
            if (s < 0.10f) {
                val p = s / 0.10f
                val head = Offset(w * (0.95f - 0.55f * p - 0.30f * n), h * (0.04f + 0.28f * p + 0.10f * n))
                val tail = Offset(head.x + w * 0.16f, head.y - h * 0.09f)
                drawLine(
                    Brush.linearGradient(listOf(Color.White, Color.Transparent), start = head, end = tail),
                    head, tail, strokeWidth = 2f * d, cap = StrokeCap.Round
                )
                drawCircle(Color.White, 2.2f * d, head)
            }
        }

        // city skyline with glowing windows
        for (b in buildings) {
            val bw = b.w * w
            val bh = b.h * h
            val left = b.x * w
            val top = h - bh
            drawRect(pal.city, Offset(left, top), Size(bw, bh))
            val cols = (bw / (9f * d)).toInt().coerceAtLeast(1)
            val rows = (bh / (12f * d)).toInt()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if ((b.i * 7 + r * 13 + c * 5) % 4 == 0) {
                        val fl = (0.55f + 0.45f * sin(TWO_PI * (t * 8 + (b.i + r + c) * 0.37f))).coerceIn(0f, 1f)
                        drawRect(
                            pal.window.copy(alpha = fl),
                            Offset(left + (c + 0.5f) * bw / cols - 1.5f * d, top + (r + 0.5f) * 12f * d),
                            Size(3f * d, 4f * d)
                        )
                    }
                }
            }
        }
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, pal.glow3), startY = h * 0.78f, endY = h))

        // falling petals (anime) / rising bubbles (ocean) / falling sparks (midnight)
        for (p in drifters) {
            val prog = (t * p.cycles + p.phase) % 1f
            val x = p.x * w + sin(TWO_PI * (t * p.swayK + p.phase)) * p.sway * d
            when (theme) {
                NOVA_OCEAN_GLASS -> {
                    val y = (1.05f - prog * 1.15f) * h
                    val r = p.size * d * 0.7f
                    drawCircle(pal.particle.copy(alpha = 0.16f), r, Offset(x, y))
                    drawCircle(pal.particle.copy(alpha = 0.65f), r, Offset(x, y), style = Stroke(width = d))
                    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.22f, Offset(x - r * 0.35f, y - r * 0.35f))
                }
                NOVA_MIDNIGHT_AMOLED -> {
                    val y = (prog * 1.15f - 0.08f) * h
                    val len = p.size * d * 3f
                    drawLine(
                        Brush.verticalGradient(listOf(Color.Transparent, pal.particle), startY = y - len, endY = y),
                        Offset(x, y - len), Offset(x, y), strokeWidth = 1.6f * d
                    )
                }
                else -> {
                    val y = (prog * 1.15f - 0.08f) * h
                    val pw = p.size * d
                    rotate(360f * ((t * p.rot) % 1f) + p.phase * 360f, Offset(x, y)) {
                        drawOval(pal.particle.copy(alpha = 0.85f), Offset(x - pw, y - pw * 0.55f), Size(pw * 2f, pw * 1.1f))
                        drawOval(Color.White.copy(alpha = 0.45f), Offset(x - pw * 0.55f, y - pw * 0.25f), Size(pw, pw * 0.5f))
                    }
                }
            }
        }

        // soft vignette so text stays readable
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.30f), Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.28f))
            )
        )
    }
}
