package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val Forest = Color(0xFF244E3C)
val Leaf = Color(0xFF58865C)
val Cream = Color(0xFFF4F7EB)
val Muted = Color(0xFF7A8A74)
val Gold = Color(0xFFF5C75D)
val Thorn = Color(0xFF665071)
val animalColors = listOf(Color(0xFFF2A04B), Color(0xFF91BA68), Color(0xFFE895AD),
    Color(0xFF8BBCE0), Color(0xFFB6A2D7), Color(0xFFE9C35F))
val animalPales = listOf(Color(0xFFFFECD5), Color(0xFFEDF4DE), Color(0xFFFBE5ED),
    Color(0xFFE4F2FB), Color(0xFFEEE8FA), Color(0xFFFFF4D2))

@Composable
fun ThornKing(modifier: Modifier = Modifier, enraged: Boolean = false) {
    Canvas(modifier) {
        val side = minOf(size.width, size.height)
        translate((size.width - side) / 2, (size.height - side) / 2) {
            scale(side / 100f, side / 100f, Offset.Zero) {
                drawOval(Color(0x22000000), Offset(14f, 88f), Size(74f, 8f))
                val spikes = Path()
                repeat(36) { i ->
                    val a = i * PI / 18
                    val r = if (i % 2 == 0) 46f else 35f
                    val x = 50f + cos(a).toFloat() * r; val y = 53f + sin(a).toFloat() * r * .84f
                    if (i == 0) spikes.moveTo(x, y) else spikes.lineTo(x, y)
                }
                spikes.close(); drawPath(spikes, if (enraged) Color(0xFF975772) else Thorn)
                drawOval(Color(0xFF43394F), Offset(17f, 22f), Size(66f, 65f))
                drawOval(Color(0xFF786084), Offset(22f, 29f), Size(56f, 53f))
                drawPath(Path().apply {
                    moveTo(29f, 12f); lineTo(39f, 22f); lineTo(50f, 5f); lineTo(61f, 22f)
                    lineTo(72f, 12f); lineTo(67f, 34f); lineTo(34f, 34f); close()
                }, Gold)
                drawCircle(Color(0xFFB17581), 4f, Offset(50f, 23f))
                drawOval(Cream, Offset(27f, 45f), Size(18f, 16f))
                drawOval(Cream, Offset(55f, 45f), Size(18f, 16f))
                drawCircle(if (enraged) Color(0xFFE16462) else Forest, 4.5f, Offset(38f, 53f))
                drawCircle(if (enraged) Color(0xFFE16462) else Forest, 4.5f, Offset(62f, 53f))
                drawLine(Color(0xFF43394F), Offset(26f, 41f), Offset(45f, 47f), 4f, StrokeCap.Round)
                drawLine(Color(0xFF43394F), Offset(55f, 47f), Offset(74f, 41f), 4f, StrokeCap.Round)
                drawArc(Cream, 12f, 156f, false, Offset(36f, 59f), Size(28f, 17f), style = Stroke(3f, cap = StrokeCap.Round))
                star(Offset(18f, 70f), 7f, Color(0xFFA9D7E8))
                star(Offset(83f, 72f), 6f, Color(0xFFA9D7E8))
            }
        }
    }
}

@Composable
fun Animal(kind: Int, modifier: Modifier = Modifier, special: Int = NORMAL) {
    Canvas(modifier) {
        val side = minOf(size.width, size.height)
        translate((size.width - side) / 2, (size.height - side) / 2) {
            scale(side / 100f, side / 100f, Offset.Zero) {
                drawAnimal(kind)
                when (special) {
                    ROW_ROCKET -> {
                        drawRoundRect(Color.White.copy(.9f), Offset(9f, 72f), Size(82f, 15f), CornerRadius(7f))
                        drawLine(Forest, Offset(22f, 79f), Offset(78f, 79f), 4f, StrokeCap.Round)
                        drawLine(Forest, Offset(24f, 79f), Offset(30f, 74f), 3f, StrokeCap.Round)
                        drawLine(Forest, Offset(76f, 79f), Offset(70f, 84f), 3f, StrokeCap.Round)
                    }
                    COL_ROCKET -> {
                        drawRoundRect(Color.White.copy(.9f), Offset(73f, 21f), Size(15f, 64f), CornerRadius(7f))
                        drawLine(Forest, Offset(80f, 31f), Offset(80f, 75f), 4f, StrokeCap.Round)
                        drawLine(Forest, Offset(80f, 31f), Offset(75f, 37f), 3f, StrokeCap.Round)
                        drawLine(Forest, Offset(80f, 75f), Offset(85f, 69f), 3f, StrokeCap.Round)
                    }
                    BOMB -> {
                        drawCircle(Gold, 15f, Offset(77f, 75f))
                        star(Offset(77f, 75f), 10f, Color.White)
                    }
                    RAINBOW -> {
                        repeat(6) { i ->
                            drawArc(animalColors[i], 188f + i * 29f, 31f, false,
                                Offset(2f, 1f), Size(96f, 96f), style = Stroke(7f, cap = StrokeCap.Round))
                        }
                        star(Offset(79f, 77f), 13f, Color.White)
                    }
                }
            }
        }
    }
}

// All six characters are original Compose vector drawings, not emoji or downloaded bitmaps.
private fun DrawScope.drawAnimal(kind: Int) {
    val body = animalColors[kind]
    val face = Color(0xFFFFF7E9)
    val ink = Color(0xFF3F403A)
    drawOval(Color(0x19000000), Offset(18f, 86f), Size(64f, 9f))
    when (kind) {
        0 -> { // fox
            val ears = Path().apply {
                moveTo(13f, 48f); lineTo(12f, 9f); quadraticTo(30f, 12f, 39f, 34f)
                moveTo(61f, 34f); quadraticTo(72f, 11f, 88f, 9f); lineTo(87f, 49f); close()
            }
            drawPath(ears, body)
            drawPath(Path().apply { moveTo(19f, 34f); lineTo(18f, 18f); lineTo(30f, 34f); close() }, Color(0xFFD98053))
            drawPath(Path().apply { moveTo(70f, 34f); lineTo(82f, 18f); lineTo(81f, 34f); close() }, Color(0xFFD98053))
            drawRoundRect(body, Offset(9f, 30f), Size(82f, 61f), CornerRadius(28f))
            drawPath(Path().apply {
                moveTo(12f, 59f); quadraticTo(29f, 58f, 50f, 77f)
                quadraticTo(70f, 58f, 88f, 59f); quadraticTo(86f, 92f, 50f, 92f)
                quadraticTo(14f, 92f, 12f, 59f); close()
            }, face)
        }
        1 -> { // frog
            drawCircle(body, 18f, Offset(27f, 30f)); drawCircle(body, 18f, Offset(73f, 30f))
            drawRoundRect(body, Offset(8f, 30f), Size(84f, 60f), CornerRadius(27f))
            drawCircle(face, 12f, Offset(27f, 28f)); drawCircle(face, 12f, Offset(73f, 28f))
            drawCircle(ink, 4f, Offset(28f, 29f)); drawCircle(ink, 4f, Offset(72f, 29f))
            drawOval(Color(0xFFC7DB99), Offset(23f, 65f), Size(54f, 22f))
        }
        2 -> { // bunny
            rotate(-12f, Offset(32f, 30f)) {
                drawRoundRect(body, Offset(21f, 0f), Size(22f, 50f), CornerRadius(11f))
                drawRoundRect(Color(0xFFF8C0CE), Offset(27f, 7f), Size(10f, 32f), CornerRadius(6f))
            }
            rotate(12f, Offset(68f, 30f)) {
                drawRoundRect(body, Offset(57f, 0f), Size(22f, 50f), CornerRadius(11f))
                drawRoundRect(Color(0xFFF8C0CE), Offset(63f, 7f), Size(10f, 32f), CornerRadius(6f))
            }
            drawRoundRect(body, Offset(10f, 34f), Size(80f, 57f), CornerRadius(28f))
            drawOval(face, Offset(30f, 63f), Size(40f, 24f))
        }
        3 -> { // bear
            drawCircle(body, 17f, Offset(22f, 24f)); drawCircle(body, 17f, Offset(78f, 24f))
            drawCircle(Color(0xFFC7DFF0), 9f, Offset(22f, 24f)); drawCircle(Color(0xFFC7DFF0), 9f, Offset(78f, 24f))
            drawRoundRect(body, Offset(9f, 27f), Size(82f, 64f), CornerRadius(28f))
            drawOval(face, Offset(31f, 59f), Size(38f, 29f))
        }
        4 -> { // owl
            drawPath(Path().apply {
                moveTo(13f, 45f); lineTo(13f, 12f); lineTo(37f, 30f); lineTo(63f, 30f)
                lineTo(87f, 12f); lineTo(87f, 45f); close()
            }, body)
            drawRoundRect(body, Offset(9f, 28f), Size(82f, 63f), CornerRadius(28f))
            drawOval(face, Offset(16f, 39f), Size(35f, 39f)); drawOval(face, Offset(49f, 39f), Size(35f, 39f))
            drawOval(Color(0xFFCCBCE8), Offset(28f, 77f), Size(44f, 11f))
        }
        5 -> { // chick
            drawOval(body, Offset(10f, 24f), Size(80f, 68f))
            drawOval(body, Offset(2f, 53f), Size(21f, 29f)); drawOval(body, Offset(77f, 53f), Size(21f, 29f))
            drawOval(body, Offset(40f, 8f), Size(13f, 30f));
            rotate(33f, Offset(53f, 27f)) { drawOval(body, Offset(50f, 10f), Size(12f, 25f)) }
        }
    }
    val eyeY = if (kind == 1) 29f else 53f
    if (kind != 1) {
        val left = if (kind == 4) 33f else 34f
        val right = if (kind == 4) 67f else 66f
        drawOval(ink, Offset(left - 3.7f, eyeY - 5f), Size(7.4f, 10f))
        drawOval(ink, Offset(right - 3.7f, eyeY - 5f), Size(7.4f, 10f))
        drawCircle(Color.White, 1.5f, Offset(left + 1f, eyeY - 2f))
        drawCircle(Color.White, 1.5f, Offset(right + 1f, eyeY - 2f))
    }
    drawOval(Color(0x60EC8B89), Offset(17f, 62f), Size(15f, 8f))
    drawOval(Color(0x60EC8B89), Offset(68f, 62f), Size(15f, 8f))
    when (kind) {
        1 -> drawArc(ink, 12f, 156f, false, Offset(35f, 46f), Size(30f, 20f), style = Stroke(2.5f, cap = StrokeCap.Round))
        4, 5 -> drawPath(Path().apply { moveTo(43f, 64f); lineTo(57f, 64f); lineTo(50f, 73f); close() }, Color(0xFFD9803D))
        else -> {
            drawOval(ink, Offset(45f, 64f), Size(10f, 7f))
            drawLine(ink, Offset(50f, 69f), Offset(50f, 75f), 2f, StrokeCap.Round)
            drawArc(ink, 0f, 180f, false, Offset(41f, 70f), Size(9f, 8f), style = Stroke(2f, cap = StrokeCap.Round))
            drawArc(ink, 0f, 180f, false, Offset(50f, 70f), Size(9f, 8f), style = Stroke(2f, cap = StrokeCap.Round))
        }
    }
    drawOval(Color.White.copy(.18f), Offset(26f, 33f), Size(28f, 9f))
}

fun DrawScope.star(center: Offset, radius: Float, color: Color) {
    val path = Path()
    repeat(10) { i ->
        val a = -PI / 2 + i * PI / 5
        val r = if (i % 2 == 0) radius else radius * .46f
        val x = center.x + cos(a).toFloat() * r; val y = center.y + sin(a).toFloat() * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close(); drawPath(path, color)
}

@Composable
fun Symbol(name: String, modifier: Modifier = Modifier, color: Color = Forest) {
    Canvas(modifier) {
        val s = minOf(size.width, size.height) / 24f
        translate((size.width - s * 24) / 2, (size.height - s * 24) / 2) {
            scale(s, s, Offset.Zero) {
                fun line(x: Float, y: Float, ex: Float, ey: Float) =
                    drawLine(color, Offset(x, y), Offset(ex, ey), 1.9f, StrokeCap.Round)
                when (name) {
                    "back" -> { line(15f, 5f, 8f, 12f); line(8f, 12f, 15f, 19f) }
                    "pause" -> { drawRoundRect(color, Offset(6f, 5f), Size(4f, 14f), CornerRadius(1f)); drawRoundRect(color, Offset(14f, 5f), Size(4f, 14f), CornerRadius(1f)) }
                    "play" -> drawPath(Path().apply { moveTo(8f, 4f); lineTo(20f, 12f); lineTo(8f, 20f); close() }, color)
                    "star" -> star(Offset(12f, 12f), 10f, color)
                    "crown" -> drawPath(Path().apply {
                        moveTo(3f, 6f); lineTo(8f, 11f); lineTo(12f, 3f); lineTo(16f, 11f)
                        lineTo(21f, 6f); lineTo(19f, 20f); lineTo(5f, 20f); close()
                    }, color)
                    "rocket" -> {
                        drawPath(Path().apply { moveTo(7f, 16f); quadraticTo(7f, 6f, 20f, 3f)
                            quadraticTo(20f, 16f, 10f, 18f); close() }, color)
                        drawCircle(Cream, 2f, Offset(14f, 9f)); line(4f, 20f, 8f, 16f)
                        line(3f, 15f, 6f, 12f); line(10f, 22f, 13f, 19f)
                    }
                    "close" -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
                    "check" -> { line(5f, 12f, 10f, 17f); line(10f, 17f, 20f, 6f) }
                    "sound" -> {
                        drawPath(Path().apply { moveTo(3f, 9f); lineTo(7f, 9f); lineTo(12f, 5f); lineTo(12f, 19f); lineTo(7f, 15f); lineTo(3f, 15f); close() }, color)
                        drawArc(color, -55f, 110f, false, Offset(9f, 3f), Size(13f, 18f), style = Stroke(1.8f, cap = StrokeCap.Round))
                    }
                    "settings" -> {
                        drawCircle(color, 7f, Offset(12f, 12f), style = Stroke(2f)); drawCircle(color, 2f, Offset(12f, 12f), style = Stroke(2f))
                        repeat(8) { i -> rotate(i * 45f, Offset(12f, 12f)) { line(12f, 2f, 12f, 5f) } }
                    }
                    "hint" -> {
                        drawCircle(color, 6f, Offset(12f, 9f), style = Stroke(1.8f))
                        line(9f, 16f, 15f, 16f); line(10f, 20f, 14f, 20f); line(12f, 10f, 12f, 15f)
                    }
                    "hammer" -> {
                        rotate(-35f, Offset(12f, 12f)) {
                            drawRoundRect(color, Offset(10f, 9f), Size(4f, 13f), CornerRadius(1.2f))
                            drawRoundRect(color, Offset(4f, 4f), Size(16f, 7f), CornerRadius(2f))
                        }
                    }
                    "shuffle" -> {
                        line(3f, 6f, 7f, 6f); line(7f, 6f, 16f, 18f); line(16f, 18f, 21f, 18f)
                        line(3f, 18f, 7f, 18f); line(7f, 18f, 16f, 6f); line(16f, 6f, 21f, 6f)
                        line(18f, 3f, 21f, 6f); line(18f, 9f, 21f, 6f)
                        line(18f, 15f, 21f, 18f); line(18f, 21f, 21f, 18f)
                    }
                    "lock" -> {
                        drawRoundRect(color, Offset(5f, 10f), Size(14f, 11f), CornerRadius(3f))
                        drawArc(color, 180f, 180f, false, Offset(8f, 2f), Size(8f, 14f), style = Stroke(2f))
                        drawCircle(Cream, 1.5f, Offset(12f, 15f))
                    }
                    "ice" -> {
                        drawRoundRect(Color(0xFFB5DCEA), Offset(3f, 3f), Size(18f, 18f), CornerRadius(5f))
                        line(6f, 7f, 13f, 5f); line(6f, 7f, 5f, 13f); line(13f, 18f, 18f, 13f)
                    }
                    "map" -> {
                        drawPath(Path().apply { moveTo(3f, 5f); lineTo(9f, 3f); lineTo(15f, 6f); lineTo(21f, 4f); lineTo(21f, 19f); lineTo(15f, 21f); lineTo(9f, 18f); lineTo(3f, 20f); close() }, color.copy(.15f))
                        line(3f, 5f, 3f, 20f); line(9f, 3f, 9f, 18f); line(15f, 6f, 15f, 21f); line(21f, 4f, 21f, 19f)
                    }
                    "sun" -> {
                        drawCircle(color, 5f, Offset(12f, 12f))
                        repeat(8) { i -> rotate(i * 45f, Offset(12f, 12f)) { line(12f, 2f, 12f, 4f) } }
                    }
                    "help" -> {
                        drawCircle(color, 9f, Offset(12f, 12f), style = Stroke(1.7f))
                        drawArc(color, 190f, 260f, false, Offset(9f, 6f), Size(6f, 7f), style = Stroke(1.7f, cap = StrokeCap.Round))
                        line(12f, 13f, 12f, 14f); drawCircle(color, 1f, Offset(12f, 18f))
                    }
                    else -> star(Offset(12f, 12f), 8f, color)
                }
            }
        }
    }
}

@Composable
fun GardenBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFFF7F8EF), Color(0xFFEDF3E4))))
        drawCircle(Color(0xFFF3EBC4).copy(.65f), w * .32f, Offset(w * .99f, h * .12f))
        drawCircle(Color(0xFFE0EBD3).copy(.65f), w * .55f, Offset(w * -.1f, h * .89f))
        repeat(12) { i ->
            val x = w * ((i * .173f + .04f) % 1f)
            val y = h * ((i * .317f + .16f) % 1f)
            rotate(i * 47f, Offset(x, y)) {
                drawOval(Leaf.copy(.07f), Offset(x, y), Size(10f + w * .025f, 25f + w * .04f))
            }
        }
    }
}
