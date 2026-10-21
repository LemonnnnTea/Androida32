@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.myapplication

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ForestApp(vm: GameViewModel, audio: GameAudio) {
    var settings by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    val s = vm.state
    val feedback = LocalHapticFeedback.current
    // Animation and sound effects are event-driven: there is no permanent game/render loop.
    LaunchedEffect(s.cueId) {
        if (s.cueId > 0 && audio.active) {
            if (vm.sound) audio.play(s.cue, s.combo)
            if (vm.haptics && s.cue in listOf("pop", "combo", "win"))
                feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    BackHandler(enabled = vm.screen != "home") {
        if (vm.screen == "game") { if (s.paused) vm.home() else vm.pause() }
        else vm.go("home")
    }
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Forest, onPrimary = Color.White, secondary = Leaf,
        background = Cream, surface = Color(0xFFFFFEF8), onSurface = Forest,
        outline = Color(0xFFDCE5D3), surfaceVariant = Color(0xFFE7EEDC)
    )) {
        Box(Modifier.fillMaxSize()) {
            GardenBackdrop(Modifier.matchParentSize())
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                when (vm.screen) {
                    "home" -> HomeScreen(vm, { settings = true }, { help = true })
                    "map" -> MapScreen(vm)
                    "game" -> GameScreen(vm)
                }
            }
            if (vm.screen == "game" && s.result.isNotEmpty()) ResultDialog(vm)
            else if (vm.screen == "game" && s.briefing) MissionBriefing(vm)
            else if (vm.screen == "game" && s.paused) PauseDialog(vm, { settings = true }, { help = true })
            if (settings) SettingsDialog(vm) { settings = false }
            if (help) HelpDialog(vm) { help = false }
        }
    }
}

@Composable
private fun HomeScreen(vm: GameViewModel, onSettings: () -> Unit, onHelp: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(Forest, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                    Symbol("star", Modifier.size(19.dp), Gold)
                }
                Text("FOREST POP", Modifier.padding(start = 9.dp), fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp, letterSpacing = 2.sp, color = Forest)
            }
            RoundIcon("help", "How to play", onHelp)
            Spacer(Modifier.width(8.dp))
            RoundIcon("settings", "Settings", onSettings)
        }
        Spacer(Modifier.height(23.dp))
        Pill("A forest to save. A trail to follow.", Color(0xFFE6EED8), Forest)
        Text("Forest Pop", Modifier.padding(top = 12.dp), color = Forest,
            fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        Text("10 missions. One forest to save.",
            Modifier.padding(top = 8.dp), color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        HeroGarden(Modifier.fillMaxWidth().height(246.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            MiniFact("map", "10 mission stages")
            MiniFact("crown", "A final boss battle")
        }
        if (vm.hasSave) {
            MainButton("Continue adventure", { vm.resume() }, "play", "continue")
            Text("${if (vm.state.mode == "daily") "Daily forest" else "Level ${vm.state.level.number}"} · ${vm.state.moves} moves left · Saved",
                Modifier.padding(top = 9.dp, bottom = 4.dp), fontSize = 11.sp, color = Muted)
            TextButton(onClick = { vm.go("map") }) { Text("Explore the level map", color = Forest) }
        } else {
            MainButton("Let's play!", { vm.start() }, "play", "play")
            TextButton(onClick = { vm.go("map") }) { Text("Explore the level map", color = Forest) }
        }
        Surface(onClick = { vm.start(daily = true) }, Modifier.fillMaxWidth().padding(top = 5.dp).testTag("daily"),
            shape = RoundedCornerShape(22.dp), color = Color(0xFFFFF8E2), border = BorderStroke(1.dp, Color(0xFFECE2BC))) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(45.dp).background(Gold.copy(.3f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Symbol("sun", Modifier.size(27.dp), Color(0xFFA97B28))
                }
                Column(Modifier.weight(1f).padding(start = 13.dp)) {
                    Text("The daily forest", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Forest)
                    Text(if (vm.dailyBest > 0) "Today's best  ${vm.dailyBest}"
                        else "Same forest for everyone, fresh each day",
                        fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 3.dp))
                }
                Text("›", fontSize = 28.sp, color = Color(0xFFA97B28))
            }
        }
        Row(Modifier.padding(top = 20.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Symbol("star", Modifier.size(15.dp), Gold)
            Text(" ${vm.stars.sum()} / ${levels.size * 3} stars collected  ·  Happily offline", fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun HeroGarden(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawOval(Color(0xFFE1EBCF), Offset(w * .03f, h * .26f), Size(w * .94f, h * .69f))
            drawOval(Color(0xFFD2E2BA), Offset(w * .10f, h * .72f), Size(w * .8f, h * .20f))
            drawCircle(Color(0xFFF5E7A9), w * .085f, Offset(w * .81f, h * .18f))
            star(Offset(w * .15f, h * .25f), 14f, Gold)
            star(Offset(w * .88f, h * .57f), 10f, Gold)
            star(Offset(w * .49f, h * .13f), 9f, Color(0xFFB0C58C))
            repeat(7) { i ->
                val x = w * (.10f + i * .13f); val y = h * (.81f + sin(i.toFloat()) * .06f)
                drawLine(Leaf.copy(.4f), Offset(x, y), Offset(x + 4f, y - 17f), 3f, StrokeCap.Round)
                drawOval(Leaf.copy(.4f), Offset(x, y - 16f), Size(13f, 7f))
            }
            listOf(.12f to .62f, .85f to .76f).forEach { (xf, yf) ->
                val p = Offset(w * xf, h * yf)
                repeat(5) { k ->
                    val a = k * 6.283f / 5f
                    drawCircle(Color(0xFFFFFCF0), 6f, p + Offset(kotlin.math.cos(a) * 7f, sin(a) * 7f))
                }
                drawCircle(Gold, 4f, p)
            }
        }
        Animal(1, Modifier.size(94.dp).offset(x = (-96).dp, y = 34.dp).rotate(-12f))
        Animal(2, Modifier.size(107.dp).offset(x = 88.dp, y = 22.dp).rotate(12f))
        Animal(0, Modifier.size(134.dp).offset(y = 7.dp))
        Pill("3 MATCH · 1 HAPPY FOREST", Color(0xFFFFFEF6), Leaf,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp), 9)
    }
}

@Composable
private fun MiniFact(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Symbol(icon, Modifier.size(17.dp), Leaf)
        Text(text, Modifier.padding(start = 5.dp), fontSize = 11.sp, color = Leaf)
    }
}

@Composable
private fun MapScreen(vm: GameViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon("back", "Back", { vm.go("home") })
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("The forest trail", color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text("10 missions. The Thorn King awaits.", color = Muted, fontSize = 10.sp)
            }
            Pill("${vm.stars.sum()} / ${levels.size * 3}", Color(0xFFF8ECC1), Color(0xFFA57B25))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            levels.forEachIndexed { i, level ->
                if (i in listOf(0, 3, 6, 9)) {
                    val chapter = when (i) { 0 -> "01  /  MEADOW MISSIONS"; 3 -> "02  /  THE FROZEN GROVE"
                        6 -> "03  /  DEEPWOOD TRIALS"; else -> "FINALE  /  THE THORN KING" }
                    Text(chapter, Modifier.padding(top = 20.dp, bottom = 12.dp), color = if (i == 9) Thorn else Leaf,
                        fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                }
                val locked = i > vm.unlocked()
                val boss = level.bossHp > 0
                Surface(onClick = { vm.start(i) }, enabled = !locked,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("level_${i + 1}"),
                    shape = RoundedCornerShape(24.dp), color = if (boss) Color(0xFFF1EAF3) else Color(0xFFFFFEF8),
                    border = BorderStroke(if (i == vm.unlocked()) 2.dp else 1.dp,
                        if (i == vm.unlocked()) { if (boss) Thorn else Leaf } else Color(0xFFDFE5D7))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(66.dp).graphicsLayer { alpha = if (locked) .48f else 1f }) {
                            if (boss) ThornKing(Modifier.fillMaxSize()) else Animal(level.goals.firstOrNull { it.type == "collect" }?.kind ?: i % KINDS, Modifier.fillMaxSize())
                        }
                        Column(Modifier.weight(1f).padding(start = 13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (boss) "FINAL BOSS" else "STAGE ${level.number.toString().padStart(2, '0')}",
                                    color = if (boss) Thorn else Leaf, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (locked) Symbol("lock", Modifier.size(16.dp), Muted) else Stars(vm.stars[i], size = 12.dp)
                            }
                            Text(level.title, Modifier.padding(top = 4.dp), color = if (boss) Thorn else Forest,
                                fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(level.description, Modifier.padding(top = 5.dp), color = Muted, fontSize = 10.sp,
                                lineHeight = 15.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("${level.moves} MOVES  /  ${if (boss) "${level.bossHp} HP" else "${level.goals.size} MISSION${if (level.goals.size > 1) "S" else ""}"}",
                                Modifier.padding(top = 8.dp), color = if (boss) Thorn else Leaf, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Text("Complete every mission to unlock the next stage.", Modifier.fillMaxWidth().padding(vertical = 20.dp),
                fontSize = 11.sp, color = Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun GameScreen(vm: GameViewModel) {
    val s = vm.state
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.width(240.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    GameHeader(vm)
                    Goals(vm)
                    if (s.level.bossHp > 0) BossPanel(vm) else ScorePanel(vm)
                    StatusMessage(vm, Modifier.height(42.dp))
                    Tools(vm)
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val boardWidth = minOf(maxWidth, (maxHeight - 20.dp) * COLS / ROWS)
                    GameBoard(vm, boardWidth)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                GameHeader(vm)
                Goals(vm)
                if (s.level.bossHp > 0) BossPanel(vm) else ScorePanel(vm)
                StatusMessage(vm, Modifier.height(43.dp))
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val boardWidth = minOf(maxWidth, (maxHeight - 12.dp).coerceAtLeast(100.dp) * COLS / ROWS)
                    GameBoard(vm, boardWidth)
                }
                Spacer(Modifier.height(10.dp))
                Tools(vm)
                Text("Swipe to swap · Or tap two neighboring friends",
                    Modifier.padding(top = 10.dp, bottom = 12.dp), color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun GameHeader(vm: GameViewModel) {
    val s = vm.state
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIcon("back", "Pause and back", { vm.pause() })
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (s.mode == "daily") "DAILY FOREST" else if (s.level.bossHp > 0) "FINAL BOSS  /  10"
                else "MISSION  ${s.level.number.toString().padStart(2, '0')}  /  ${levels.size}",
                color = Leaf, letterSpacing = 1.6.sp, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(s.level.title, color = Forest, fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 3.dp))
        }
        RoundIcon("pause", "Pause", { vm.pause() }, "pause")
    }
}

@Composable
private fun Goals(vm: GameViewModel) {
    val s = vm.state
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color(0xFFFFFEF8), border = BorderStroke(1.dp, Color(0xFFE1E8D8))) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MOVES", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                Text("${s.moves}", color = if (s.moves <= 5) Color(0xFFD77E60) else Forest,
                    fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 35.sp, modifier = Modifier.testTag("moves"))
            }
            Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(42.dp).background(Color(0xFFE5EBDD)))
            Column(Modifier.weight(1f)) {
                Text("YOUR MISSIONS", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                if (s.level.bossHp > 0) {
                    Text("Defeat the Thorn King", Modifier.padding(top = 7.dp), color = Thorn, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else FlowRow(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp), maxItemsInEachRow = 3) {
                    s.level.goals.forEach { GoalChip(s, it) }
                }
            }
        }
    }
}

@Composable
private fun GoalIcon(goal: Goal, modifier: Modifier = Modifier) {
    if (goal.type == "collect") Animal(goal.kind, modifier)
    else Symbol(when (goal.type) { "ice" -> "ice"; "make" -> "star"; "activate" -> "rocket"
        "cascade" -> "shuffle"; "boss" -> "crown"; else -> "star" }, modifier,
        if (goal.type == "boss") Thorn else if (goal.type in listOf("make", "score")) Gold else Leaf)
}

@Composable
private fun GoalChip(s: GameState, goal: Goal) {
    val progress = goalProgress(s, goal).coerceAtMost(goal.count)
    val done = progress >= goal.count
    Row(Modifier.background(if (done) Color(0xFFE2EED5) else Cream, RoundedCornerShape(10.dp)).padding(horizontal = 5.dp, vertical = 3.dp)
        .testTag("goal_${goal.type}_${goal.kind}").semantics { contentDescription = "${goalLabel(goal)}: $progress/${goal.count}" },
        verticalAlignment = Alignment.CenterVertically) {
        GoalIcon(goal, Modifier.size(23.dp))
        Column(Modifier.padding(start = 3.dp)) {
            if (goal.type != "collect") Text(when (goal.type) { "make" -> "Make"; "activate" -> "Use"; "cascade" -> "Chain"
                "ice" -> "Ice"; else -> "Score" }, fontSize = 8.sp, color = Muted, lineHeight = 9.sp)
            Text("$progress/${goal.count}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Forest)
        }
        if (done) Symbol("check", Modifier.padding(start = 2.dp).size(11.dp), Leaf)
    }
}

@Composable
private fun MissionRow(s: GameState, goal: Goal, showProgress: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        GoalIcon(goal, Modifier.size(31.dp))
        Text(goalLabel(goal), Modifier.weight(1f).padding(start = 10.dp), color = Forest, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        if (showProgress) Text("${goalProgress(s, goal).coerceAtMost(goal.count)}/${goal.count}",
            color = Leaf, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BossPanel(vm: GameViewModel) {
    val s = vm.state
    val rage = bossAttackInterval(s) == 2
    val health by animateFloatAsState(s.bossHealth.toFloat() / s.level.bossHp, tween(320), label = "boss-hp")
    Surface(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(20.dp), color = Color(0xFFF0EAF2),
        border = BorderStroke(1.dp, Color(0xFFD9CDDF))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThornKing(Modifier.size(48.dp), rage)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (rage) "ENRAGED" else "BOSS HEALTH", color = if (rage) Color(0xFFBB6375) else Thorn,
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${s.bossHealth} / ${s.level.bossHp} HP", color = Thorn, fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold, modifier = Modifier.testTag("boss_health"))
                    }
                    Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(7.dp).background(Color(0xFFDCD1E1), CircleShape)) {
                        Box(Modifier.fillMaxWidth(health).fillMaxHeight().background(if (rage) Color(0xFFBB6375) else Thorn, CircleShape))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Animal(s.bossWeakness, Modifier.size(22.dp))
                        Text("${animalName(s.bossWeakness)} x4", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Thorn, modifier = Modifier.weight(1f))
                        Text("Frost in ${(bossAttackInterval(s) - s.bossTurns).coerceAtLeast(1)}", color = Thorn, fontSize = 9.sp)
                    }
                }
            }
            Text("Special +10  /  Cascade +6  /  Ice blocks color hits", Modifier.padding(top = 4.dp), fontSize = 8.sp, color = Thorn)
        }
    }
}

@Composable
private fun ScorePanel(vm: GameViewModel) {
    val s = vm.state
    val progress by animateFloatAsState((s.score.toFloat() / (s.level.target * 2)).coerceIn(0f, 1f), tween(350), label = "score")
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, start = 5.dp, end = 5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("FOREST ENERGY", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Medium)
            Text("  ${s.score}", fontSize = 17.sp, color = Forest, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f).testTag("score"))
            Stars(when { s.score >= s.level.target * 2 -> 3; s.score >= s.level.target * 1.4 -> 2; s.score >= s.level.target -> 1; else -> 0 }, size = 16.dp)
        }
        Box(Modifier.fillMaxWidth().padding(top = 7.dp).height(7.dp).background(Color(0xFFDDE7CD), CircleShape)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFFA8C57E), Leaf)), CircleShape))
        }
    }
}

@Composable
private fun StatusMessage(vm: GameViewModel, modifier: Modifier = Modifier) {
    val s = vm.state
    val message = when (s.toast) {
        "welcome" -> if (s.level.bossHp > 0) "Unfrozen ${animalName(s.bossWeakness)} matches hit the king!" else "Complete the missions above to clear this stage."
        "invalid" -> "So close! A swap needs to make a match of 3."
        "hint" -> "Try swapping the two golden friends."
        "hammer" -> "Tap a friend to pop it. No move needed!"
        "reshuffle" -> "A little forest magic. Mixing things up!"
        "combo" -> "${s.combo}× COMBO!  Lovely!  +${s.toastPoints}"
        "match" -> "Happy little match  +${s.toastPoints}"
        "boss_hit" -> "Direct hit!  -${s.toastPoints} HP"
        "boss_attack" -> "Frost spreads! New weakness: ${animalName(s.bossWeakness)}"
        else -> if (s.hammerMode) "Choose a friend to pop" else ""
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(message, color = if (s.toast.startsWith("boss")) Thorn else if (s.combo > 1 && s.toast == "combo") Color(0xFFBA8243) else Leaf,
            fontSize = if (s.combo > 1 && s.toast == "combo") 15.sp else 11.sp,
            fontWeight = if (s.combo > 1) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GameBoard(vm: GameViewModel, width: Dp) {
    val s = vm.state
    val padding = 7.dp
    val cell = (width - padding * 2) / COLS
    val height = cell * ROWS + padding * 2
    Box(Modifier.width(width).height(height).shadow(5.dp, RoundedCornerShape(24.dp), ambientColor = Forest.copy(.1f), spotColor = Forest.copy(.12f))
        .background(Color(0xFFCADBB4), RoundedCornerShape(24.dp))
        .border(1.5.dp, Color(0xFFB8CEA0), RoundedCornerShape(24.dp)).testTag("board")) {
        Box(Modifier.padding(padding).fillMaxSize()) {
            repeat(COLS * ROWS) { i ->
                Box(Modifier.offset(cell * (i % COLS), cell * (i / COLS)).size(cell).padding(2.dp)
                    .background(if (i in s.ice) Color(0xFFD5EDF5) else if ((i / COLS + i % COLS) % 2 == 0)
                        Color(0xFFE5EDDA) else Color(0xFFEAF0DF), RoundedCornerShape(11.dp)))
            }
            s.board.forEachIndexed { i, gem ->
                if (gem != null) key(gem.id) {
                    BoardGem(vm, gem, i, cell, i == s.selected, i in s.hint, i in s.removing, i in s.ice)
                }
            }
            MatchParticles(s, cell)
        }
    }
}

@Composable
private fun BoardGem(vm: GameViewModel, gem: Gem, index: Int, cell: Dp,
    selected: Boolean, hint: Boolean, removing: Boolean, iced: Boolean) {
    val x by animateDpAsState(cell * (index % COLS), tween(180), label = "tile-x")
    val y by animateDpAsState(cell * (index / COLS), tween(210), label = "tile-y")
    val entrance = remember { Animatable(.15f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, spring(dampingRatio = .7f, stiffness = 600f)) }
    val scale by animateFloatAsState(if (removing) .03f else if (selected || hint) 1.08f else 1f, tween(170), label = "tile-pop")
    val alpha by animateFloatAsState(if (removing) 0f else 1f, tween(170), label = "tile-alpha")
    // Pointer input is keyed by cell position, never by selection/busy state: an active drag is stable.
    val currentIndex by rememberUpdatedState(index)
    val colors = if (iced) Color(0xFFD4EDF6) else animalPales[gem.kind]
    Box(Modifier.offset { IntOffset(x.roundToPx(), y.roundToPx()) }.size(cell)
        .graphicsLayer { scaleX = scale * entrance.value; scaleY = scale * entrance.value; this.alpha = alpha }
        .padding(2.dp)
        .shadow(if (selected || hint) 4.dp else 1.dp, RoundedCornerShape(11.dp), spotColor = Forest.copy(.15f))
        .background(colors, RoundedCornerShape(11.dp))
        .border(if (selected || hint) 2.2.dp else 1.dp,
            if (selected) Forest else if (hint) Color(0xFFE6B43E) else Color.White.copy(.8f), RoundedCornerShape(11.dp))
        .testTag("cell_$index")
        .semantics { contentDescription = "${animalName(gem.kind)}, row ${index / COLS + 1}, column ${index % COLS + 1}" }
        .clickable(enabled = !vm.state.busy && !vm.state.paused) { vm.tap(currentIndex) }
        .pointerInput(gem.id) {
            var drag = Offset.Zero
            detectDragGestures(onDragStart = { drag = Offset.Zero }, onDragCancel = { drag = Offset.Zero },
                onDragEnd = {
                    val threshold = cell.toPx() * .22f
                    if (abs(drag.x) > threshold || abs(drag.y) > threshold) {
                        if (abs(drag.x) > abs(drag.y)) vm.swipe(currentIndex, if (drag.x > 0) 1 else -1, 0)
                        else vm.swipe(currentIndex, 0, if (drag.y > 0) 1 else -1)
                    }
                }) { change, amount -> change.consume(); drag += amount }
        }, contentAlignment = Alignment.Center) {
        Animal(gem.kind, Modifier.fillMaxSize().padding(3.dp), gem.special)
        if (iced) Canvas(Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            drawLine(Color.White.copy(.9f), Offset(w * .13f, h * .29f), Offset(w * .20f, h * .13f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(Color.White.copy(.9f), Offset(w * .20f, h * .13f), Offset(w * .43f, h * .10f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(Color.White.copy(.9f), Offset(w * .76f, h * .9f), Offset(w * .9f, h * .77f), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun MatchParticles(s: GameState, cell: Dp) {
    val progress = remember { Animatable(1f) }
    var origins by remember { mutableStateOf(emptyList<Int>()) }
    LaunchedEffect(s.cueId) {
        if (s.removing.isNotEmpty()) {
            origins = s.removing.toList()
            progress.snapTo(0f)
            progress.animateTo(1f, tween(430))
            origins = emptyList()
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val p = progress.value
        val c = cell.toPx()
        origins.forEach { i ->
            val center = Offset((i % COLS + .5f) * c, (i / COLS + .5f) * c)
            repeat(5) { j ->
                val a = j * 6.283f / 5f + i
                val r = c * .65f * p
                star(center + Offset(kotlin.math.cos(a) * r, sin(a) * r),
                    c * .075f * (1f - p), if (j % 2 == 0) Gold.copy(1f - p) else Color.White.copy(1f - p))
            }
        }
    }
}

@Composable
private fun Tools(vm: GameViewModel) {
    val s = vm.state
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        ToolButton("hint", "Hint", "${s.hints}", s.hints > 0, false, Modifier.weight(1f)) { vm.hint() }
        ToolButton("hammer", "Pop", "${s.hammers}", s.hammers > 0, s.hammerMode, Modifier.weight(1f)) { vm.toggleHammer() }
        ToolButton("shuffle", "Mix", "${s.shuffles}", s.shuffles > 0, false, Modifier.weight(1f)) { vm.shuffle() }
    }
}

@Composable
private fun ToolButton(icon: String, label: String, count: String, enabled: Boolean,
    selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.testTag("tool_$icon"),
        shape = RoundedCornerShape(20.dp), color = if (selected) Color(0xFFD9E8C8) else Color(0xFFFFFEF7),
        border = BorderStroke(1.dp, if (selected) Leaf else Color(0xFFE0E7D7))) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Symbol(icon, Modifier.size(23.dp), if (enabled) Leaf else Muted.copy(.35f))
                Text(count, Modifier.padding(start = 6.dp).background(Color(0xFFEBF0E2), CircleShape).padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Muted)
            }
            Text(label, Modifier.padding(top = 5.dp), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (enabled) Forest else Muted.copy(.5f))
        }
    }
}

@Composable
private fun Stars(count: Int, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i -> Symbol("star", Modifier.size(size), if (i < count) Gold else Color(0xFFDDE5D2)) }
    }
}

@Composable
private fun RoundIcon(icon: String, description: String, onClick: () -> Unit, tag: String = icon) {
    Surface(onClick = onClick, modifier = Modifier.size(44.dp).testTag(tag).semantics { contentDescription = description },
        shape = CircleShape, color = Color(0xFFFFFEF6).copy(.9f), border = BorderStroke(1.dp, Color(0xFFE1E8D7))) {
        Box(contentAlignment = Alignment.Center) { Symbol(icon, Modifier.size(20.dp), Forest) }
    }
}

@Composable
private fun Pill(text: String, background: Color, color: Color, modifier: Modifier = Modifier, fontSize: Int = 10) {
    Text(text, modifier.background(background, CircleShape).padding(horizontal = 13.dp, vertical = 7.dp),
        color = color, fontSize = fontSize.sp, fontWeight = FontWeight.Medium, letterSpacing = .3.sp)
}

@Composable
private fun MainButton(text: String, onClick: () -> Unit, icon: String = "play", tag: String = "main_button") {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(57.dp).testTag(tag),
        shape = RoundedCornerShape(19.dp), colors = ButtonDefaults.buttonColors(containerColor = Forest),
        contentPadding = PaddingValues(horizontal = 22.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Symbol(icon, Modifier.size(21.dp), Color(0xFFD6E8BC))
    }
}

@Composable
private fun ForestDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp), shape = RoundedCornerShape(30.dp),
            color = Color(0xFFFBFCF3), border = BorderStroke(1.dp, Color.White)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(25.dp),
                horizontalAlignment = Alignment.CenterHorizontally, content = content)
        }
    }
}

@Composable
private fun MissionBriefing(vm: GameViewModel) {
    val s = vm.state
    val boss = s.level.bossHp > 0
    ForestDialog(onDismiss = { vm.home() }) {
        Pill(if (boss) "FINAL BOSS / STAGE 10" else if (s.mode == "daily") "DAILY MISSION" else "STAGE ${s.level.number} / ${levels.size}",
            if (boss) Color(0xFFEAE0EE) else Color(0xFFE5EED9), if (boss) Thorn else Leaf)
        if (boss) ThornKing(Modifier.padding(top = 14.dp).size(100.dp))
        else GoalIcon(s.level.goals.first(), Modifier.padding(top = 14.dp).size(75.dp))
        Text(s.level.title, Modifier.padding(top = 12.dp), color = if (boss) Thorn else Forest,
            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(s.level.description, Modifier.padding(top = 8.dp, bottom = 14.dp), color = Muted,
            fontSize = 12.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
        Column(Modifier.fillMaxWidth().background(Color(0xFFEBF1E2), RoundedCornerShape(18.dp)).padding(12.dp)) {
            s.level.goals.forEach { MissionRow(s, it) }
        }
        Text("${s.level.moves} moves. Complete every mission.", Modifier.padding(top = 13.dp),
            color = if (boss) Thorn else Forest, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (boss) Text("Unfrozen weak-color friends deal 4 damage each. Activated specials add 10; extra cascades add 6.\nEach attack changes his weakness. Frost spreads to 2 tiles every 3 moves, then 3 tiles every 2 moves at half HP.",
            Modifier.padding(top = 10.dp), color = Muted, fontSize = 10.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
        else Text("Score earns stars. Only the missions above decide victory.", Modifier.padding(top = 9.dp), color = Muted,
            fontSize = 10.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        MainButton(if (boss) "Challenge the king" else "Begin mission", { vm.beginLevel() }, if (boss) "crown" else "play", "begin_level")
    }
}

@Composable
private fun PauseDialog(vm: GameViewModel, onSettings: () -> Unit, onHelp: () -> Unit) {
    var confirmRestart by remember { mutableStateOf(false) }
    ForestDialog(onDismiss = { vm.unpause() }) {
        Animal(3, Modifier.size(86.dp))
        Text("A little breather", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Forest, modifier = Modifier.padding(top = 12.dp))
        Text("Your forest friends will be right here.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 23.dp))
        MainButton("Keep playing", { vm.unpause() }, tag = "resume")
        Row(Modifier.fillMaxWidth().padding(top = 11.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = onSettings) { Text("Settings", color = Leaf) }
            TextButton(onClick = onHelp) { Text("How to play", color = Leaf) }
        }
        HorizontalDivider(color = Color(0xFFE1E8D7), modifier = Modifier.padding(vertical = 8.dp))
        TextButton(onClick = { confirmRestart = true }) { Text("Restart this level", color = Muted) }
        TextButton(onClick = { vm.home() }, modifier = Modifier.testTag("save_home")) {
            Text("Save & go home", color = Leaf)
        }
        if (confirmRestart) {
            Text("Start this level again? Current progress will reset.", fontSize = 12.sp, color = Forest, textAlign = TextAlign.Center)
            Row {
                TextButton(onClick = { confirmRestart = false }) { Text("Cancel") }
                TextButton(onClick = { vm.restart() }) { Text("Restart") }
            }
        }
    }
}

@Composable
private fun ResultDialog(vm: GameViewModel) {
    val s = vm.state
    val won = s.result == "won"
    val finale = s.mode == "story" && s.level.bossHp > 0
    ForestDialog(onDismiss = { vm.go("home") }) {
        if (won) Stars(vm.rating(), size = 46.dp)
        if (finale && !won) ThornKing(Modifier.padding(top = 12.dp).size(90.dp), true)
        else Animal(if (won) 0 else 2, Modifier.padding(top = 12.dp).size(90.dp))
        Text(if (won && finale) "The forest is free!" else if (won) "Mission complete!" else "So very close!",
            Modifier.padding(top = 14.dp), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = if (finale) Thorn else Forest, textAlign = TextAlign.Center)
        Text(if (won && finale) "The Thorn King has fallen. You saved the forest."
            else if (won) "Every mission complete. Your next adventure is waiting."
            else "Plan your next attempt around the unfinished missions.",
            Modifier.padding(top = 8.dp, bottom = 18.dp), color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth().background(Color(0xFFEBF1E1), RoundedCornerShape(18.dp)).padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SCORE", color = Muted, fontSize = 10.sp)
                Text("${s.score}", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Forest)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BEST COMBO", color = Muted, fontSize = 10.sp)
                Text("${s.bestCombo}x", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Forest)
            }
        }
        if (won) Text("Moves-left bonus  +${s.toastPoints}", color = Leaf, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
        else Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Text("UNFINISHED MISSIONS", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            s.level.goals.filter { goalProgress(s, it) < it.count }.forEach { MissionRow(s, it, true) }
        }
        Spacer(Modifier.height(20.dp))
        val hasNext = won && s.mode == "story" && s.level.number < levels.size
        MainButton(if (hasNext) "Next mission!" else if (won && finale) "See the finished trail" else "Try this mission again",
            { if (hasNext) vm.start(s.level.number) else if (won && finale) vm.go("map") else vm.restart() }, tag = "result_action")
        TextButton(onClick = { vm.go("home") }) { Text("Back to the forest", color = Leaf) }
    }
}

@Composable
private fun SettingsDialog(vm: GameViewModel, onDismiss: () -> Unit) {
    ForestDialog(onDismiss) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Make it yours", Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, color = Forest, fontSize = 23.sp)
            RoundIcon("close", "Close", onDismiss)
        }
        Spacer(Modifier.height(18.dp))
        SettingsRow("Happy little sounds", "Soft pops and a little victory melody", vm.sound, { vm.updateSound(it) })
        SettingsRow("Feel the matches", "A gentle tap for each happy match", vm.haptics, { vm.updateHaptics(it) })
        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Color(0xFFE1E8D7))
        Text("FOREST POP  1.2\n10 missions · The Thorn King · Auto-save",
            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 5.dp), color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Forest)
            Text(subtitle, fontSize = 10.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
        }
        Switch(checked, onCheckedChange = onChange)
    }
}

@Composable
private fun HelpDialog(vm: GameViewModel, onDismiss: () -> Unit) {
    ForestDialog(onDismiss) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("A tiny field guide", Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, color = Forest, fontSize = 22.sp)
            RoundIcon("close", "Close", onDismiss)
        }
        Row(Modifier.padding(vertical = 17.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(3) { Animal(0, Modifier.size(57.dp)) } }
        HelpLine("01", "Three friends make a match",
            "Swipe two neighbors, or tap them one after the other. Match 3 or more in a row or column. Invalid swaps never cost a move.")
        HelpLine("02", "Bigger matches, bigger magic",
            "Match 4 for a line rocket, 5 for a rainbow, or a T/L shape for a 3×3 bomb. Match a special to trigger it, or swap two specials. Swap a rainbow with any friend to clear that color.")
        HelpLine("03", "A little goal to grow toward",
            "Each stage has its own missions: collect specific friends, break ice, create or activate special pieces, or trigger extra cascade waves. Complete ALL listed missions within the move limit. Score earns stars; it is only required when a score mission is listed.")
        HelpLine("BOSS", "The Thorn King",
            "Stage 10 is a 240 HP boss battle. Unfrozen friends of his weakness color deal 4 damage each; activated specials add 10, and each extra cascade adds 6. Ice blocks color damage on that clear. His frost attacks change the weakness, and he attacks faster at half health.")
        HelpLine("04", "A helping paw",
            "Plan carefully: each game gives you $START_HINTS hints, $START_HAMMERS pop and $START_SHUFFLES mix, with no move cost. Stuck boards mix automatically for free. The daily puzzle starts with the same board all day.")
        HelpLine("★", "A little extra sparkle",
            "Completing every mission earns at least 1 star. Reach 1.4x the stage's score benchmark for 2 stars, or 2x for 3 stars. Each leftover move adds $MOVE_BONUS points. Cascades multiply the points per friend!")
        Spacer(Modifier.height(15.dp))
        MainButton("Got it. Let's play!", onDismiss, "check")
    }
}

@Composable
private fun HelpLine(number: String, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Text(number, color = Leaf, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
        Column {
            Text(title, color = Forest, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(body, color = Muted, fontSize = 11.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}
