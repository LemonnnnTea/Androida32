package com.example.myapplication

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.edit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("forest_pop_v1", 0)
    private var engine = MatchEngine()
    private var turn: Job? = null
    var state by mutableStateOf(GameState()); private set
    var screen by mutableStateOf("home"); private set
    var sound by mutableStateOf(prefs.getBoolean("sound", true)); private set
    var haptics by mutableStateOf(prefs.getBoolean("haptics", true)); private set
    // Earlier stars can unlock stages 1-9, but the new finale must be won as a boss battle.
    var stars by mutableStateOf(List(levels.size) { prefs.getInt("campaign3_stars_$it",
        if (it < levels.lastIndex) prefs.getInt("stars_$it", 0) else 0) }); private set
    var bestScores by mutableStateOf(List(levels.size) { prefs.getInt("campaign3_best_$it",
        if (it < levels.lastIndex) prefs.getInt("best_$it", 0) else 0) }); private set
    var dailyBest by mutableIntStateOf(prefs.getInt("daily_v2_${today()}", 0)); private set
    var hasSave by mutableStateOf(false); private set

    init { restore() }

    fun unlocked() = stars.indexOfFirst { it == 0 }.let { if (it == -1) levels.lastIndex else it }
    fun go(destination: String) { screen = destination }
    fun updateSound(value: Boolean) { sound = value; prefs.edit { putBoolean("sound", value) } }
    fun updateHaptics(value: Boolean) { haptics = value; prefs.edit { putBoolean("haptics", value) } }

    fun start(index: Int = unlocked(), daily: Boolean = false) {
        if (!daily && index > unlocked()) return
        turn?.cancel()
        val day = today()
        engine = if (daily) MatchEngine(Random(day.toInt())) else MatchEngine()
        val level = if (daily) dailyLevel(day) else levels[index.coerceIn(0, levels.lastIndex)]
        state = GameState(board = engine.fresh(), level = level, moves = level.moves,
            ice = level.ice, mode = if (daily) "daily" else "story", day = day, briefing = true)
        screen = "game"
        checkpoint()
    }

    fun resume() {
        if (hasSave) { state = state.copy(paused = false); screen = "game" }
    }

    fun pause() {
        if (screen == "game" && state.result.isEmpty()) state = state.copy(paused = true, selected = -1)
        checkpoint()
    }

    fun unpause() { state = state.copy(paused = false) }
    fun beginLevel() { state = state.copy(briefing = false, paused = false) }
    fun home() { pause(); screen = "home" }
    fun restart() { start((state.level.number - 1).coerceAtLeast(0), state.mode == "daily") }

    fun tap(index: Int) {
        val s = state
        if (s.busy || s.paused || s.briefing || s.result.isNotEmpty() || index !in s.board.indices) return
        if (s.hammerMode) { hammer(index); return }
        if (s.selected == index) { state = s.copy(selected = -1); return }
        if (adjacent(s.selected, index)) swap(s.selected, index)
        else state = s.copy(selected = index, hint = emptySet())
    }

    fun swipe(from: Int, dx: Int, dy: Int) {
        val r = from / COLS + dy; val c = from % COLS + dx
        if (r in 0 until ROWS && c in 0 until COLS) swap(from, r * COLS + c)
    }

    fun swap(a: Int, b: Int) {
        val s = state
        if (s.busy || s.paused || s.briefing || s.result.isNotEmpty() || !adjacent(a, b)) return
        val valid = engine.legal(s.board, a, b)
        turn = viewModelScope.launch {
            state = state.copy(board = swapped(s.board, a, b), selected = -1,
                hint = emptySet(), busy = true, hammerMode = false, toast = "", toastPoints = 0)
            delay(200)
            if (!valid) {
                state = state.copy(board = s.board, toast = "invalid")
                cue("invalid")
                delay(180)
                state = state.copy(busy = false)
            } else {
                state = state.copy(moves = s.moves - 1, combo = 0)
                val effect = engine.swapEffect(state.board, a, b)
                settle(effect?.let { MatchPlan(it, activatedRainbows = setOf(a, b).filter { i -> state.board[i]?.special == RAINBOW }.toSet()) }
                    ?: engine.plan(state.board, listOf(b, a)), spentMove = true)
            }
        }
    }

    private fun cue(name: String) { state = state.copy(cue = name, cueId = state.cueId + 1) }

    private suspend fun settle(first: MatchPlan, spentMove: Boolean = false) {
        var plan = first
        var chain = 0
        while (plan.clear.isNotEmpty()) {
            chain++
            val before = state
            val cleared = engine.expand(before.board, plan.clear, plan.create.keys, plan.activatedRainbows)
            state = recordWave(before, cleared, plan.create, chain)
            cue(if (chain > 1) "combo" else "pop")
            delay(210)
            val holes = state.board.mapIndexed { i, gem ->
                when { i in plan.create -> gem?.copy(special = plan.create.getValue(i))
                    i in cleared -> null
                    else -> gem }
            }
            state = state.copy(board = holes, removing = emptySet())
            delay(55)
            state = state.copy(board = engine.fall(holes))
            delay(230)
            plan = engine.plan(state.board)
            // Random refill chains are finite in practice; a bound also prevents pathological loops.
            if (chain >= 40) {
                state = state.copy(board = engine.fresh())
                break
            }
        }
        val resolved = finishTurn(state, spentMove)
        state = resolved
        if (resolved.result == "won") {
            recordWin()
            cue("win")
        } else if (resolved.result == "lost") {
            cue("lose")
        } else {
            state = state.copy(busy = true)
            if (resolved.toast == "boss_attack") {
                cue("invalid")
                delay(360)
            }
            if (engine.hint(state.board) == null) {
                state = state.copy(toast = "reshuffle")
                delay(400)
                state = state.copy(board = engine.shuffle(state.board))
                delay(200)
            }
            state = state.copy(busy = false)
        }
        if (state.mode == "daily") {
            val key = "daily_v2_${state.day}"
            val best = maxOf(prefs.getInt(key, 0), state.score)
            prefs.edit { putInt(key, best) }
            if (state.day == today()) dailyBest = best
        }
        checkpoint()
    }

    fun rating(): Int = when {
        state.score >= state.level.target * 2 -> 3
        state.score >= state.level.target * 1.4 -> 2
        else -> 1
    }

    private fun recordWin() {
        if (state.mode != "story") return
        val i = state.level.number - 1
        stars = stars.toMutableList().also { it[i] = maxOf(it[i], rating()) }
        bestScores = bestScores.toMutableList().also { it[i] = maxOf(it[i], state.score) }
        prefs.edit { putInt("campaign3_stars_$i", stars[i]); putInt("campaign3_best_$i", bestScores[i]) }
    }

    fun hint() {
        if (state.busy || state.paused || state.briefing || state.result.isNotEmpty() || state.hints <= 0 || state.hint.isNotEmpty()) return
        val pair = engine.hint(state.board) ?: return
        state = state.copy(hint = setOf(pair.first, pair.second), selected = -1,
            hints = state.hints - 1, hammerMode = false, toast = "hint", toastPoints = 0)
        checkpoint()
    }

    fun toggleHammer() {
        if (state.busy || state.paused || state.briefing || state.result.isNotEmpty() || state.hammers <= 0) return
        state = state.copy(hammerMode = !state.hammerMode, selected = -1, hint = emptySet(),
            toast = if (!state.hammerMode) "hammer" else "", toastPoints = 0)
    }

    private fun hammer(at: Int) {
        if (state.hammers <= 0) return
        state = state.copy(hammerMode = false, hammers = state.hammers - 1, busy = true)
        turn = viewModelScope.launch { settle(MatchPlan(setOf(at))) }
    }

    fun shuffle() {
        if (state.busy || state.paused || state.briefing || state.result.isNotEmpty() || state.shuffles <= 0) return
        state = state.copy(busy = true, shuffles = state.shuffles - 1,
            selected = -1, hint = emptySet(), hammerMode = false, toast = "reshuffle", toastPoints = 0)
        turn = viewModelScope.launch {
            cue("combo")
            delay(180)
            state = state.copy(board = engine.shuffle(state.board))
            delay(240)
            state = state.copy(busy = false)
            checkpoint()
        }
    }

    // Only stable boards are written. A process killed mid-animation resumes the previous turn.
    // SharedPreferences is private local storage; no network or storage permission is needed.
    private fun checkpoint() {
        val s = state
        if (s.busy || s.board.size != ROWS * COLS) return
        if (s.result.isNotEmpty()) {
            hasSave = false
            prefs.edit { remove("run") }
            return
        }
        val json = JSONObject().put("version", 3).put("level", s.level.number)
            .put("mode", s.mode).put("day", s.day).put("moves", s.moves)
            .put("score", s.score).put("collections", JSONArray(s.collections)).put("hints", s.hints).put("hammers", s.hammers)
            .put("shuffles", s.shuffles).put("bestCombo", s.bestCombo)
            .put("specialsMade", s.specialsMade).put("specialsUsed", s.specialsUsed).put("cascadeWaves", s.cascadeWaves)
            .put("bossHealth", s.bossHealth).put("bossWeakness", s.bossWeakness).put("bossTurns", s.bossTurns)
            .put("ice", JSONArray(s.ice.toList()))
            .put("board", JSONArray(s.board.map { gem ->
                requireNotNull(gem)
                JSONObject().put("id", gem.id).put("kind", gem.kind).put("special", gem.special)
            }))
        prefs.edit { putString("run", json.toString()) }
        hasSave = true
    }

    private fun restore() {
        val raw = prefs.getString("run", null) ?: return
        runCatching {
            val j = JSONObject(raw)
            val version = j.getInt("version")
            require(version in 1..3)
            val mode = j.getString("mode")
            require(mode == "story" || mode == "daily")
            val day = j.getLong("day")
            val oldNumber = j.getInt("level")
            val level = if (mode == "daily") dailyLevel(day) else levels[(oldNumber - 1).coerceIn(0, levels.lastIndex)]
            val gems = j.getJSONArray("board")
            require(gems.length() == COLS * ROWS)
            val board = List(gems.length()) { i ->
                val g = gems.getJSONObject(i)
                Gem(g.getLong("id"), g.getInt("kind"), g.getInt("special"))
            }
            require(board.all { it.kind in 0 until KINDS && it.special in 0..BOMB })
            require(board.map { it.id }.distinct().size == board.size)
            val iceJson = j.getJSONArray("ice")
            val savedIce = (0 until iceJson.length()).map { iceJson.getInt(it) }.toSet()
            val legacyIce = if (mode == "daily") (0 until COLS * ROWS)
                .filter { (it + day) % (if (version == 1) 5L else 4L) == 0L }.toSet()
                else if (oldNumber - 1 < (if (version == 1) 3 else 2)) emptySet()
                else (0 until COLS * ROWS).filter { (it / COLS + it % COLS + oldNumber - 1) % 3 == 0 }
                    .take(5 + oldNumber).toSet()
            val ice = if (version < 3) level.ice - (legacyIce - savedIce) else savedIce
            val collectionsJson = j.optJSONArray("collections")
            val legacyKind = if (mode == "daily") (day % KINDS).toInt() else (oldNumber - 1).coerceAtLeast(0) % KINDS
            val collections = if (version == 3) {
                require(collectionsJson != null && collectionsJson.length() == KINDS)
                List(KINDS) { collectionsJson.getInt(it).coerceAtLeast(0) }
            } else List(KINDS) { if (it == legacyKind) j.optInt("collected", 0) else 0 }
            state = GameState(board = board, level = level, mode = mode, day = day,
                moves = j.getInt("moves").coerceIn(1, level.moves), score = j.getInt("score"),
                collections = collections, hints = j.optInt("hints", START_HINTS).coerceIn(0, START_HINTS),
                hammers = j.getInt("hammers").coerceIn(0, START_HAMMERS),
                shuffles = j.getInt("shuffles").coerceIn(0, START_SHUFFLES), bestCombo = j.optInt("bestCombo"), ice = ice,
                specialsMade = j.optInt("specialsMade"), specialsUsed = j.optInt("specialsUsed"),
                cascadeWaves = j.optInt("cascadeWaves"),
                bossHealth = j.optInt("bossHealth", level.bossHp).coerceIn(0, level.bossHp),
                bossWeakness = j.optInt("bossWeakness").coerceIn(0, KINDS - 1), bossTurns = j.optInt("bossTurns"))
            engine.adopt(board)
            hasSave = true
            if (version < 3) checkpoint()
        }.onFailure { prefs.edit { remove("run") } }
    }
}

// Calendar supports the project's minSdk 24 without a java.time desugaring dependency.
fun today(): Long = Calendar.getInstance().let {
    it.get(Calendar.YEAR) * 1000L + it.get(Calendar.DAY_OF_YEAR)
}
