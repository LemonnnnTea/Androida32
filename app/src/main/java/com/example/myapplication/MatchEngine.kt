package com.example.myapplication

import kotlin.math.abs
import kotlin.random.Random

// Original assignment implementation: no game engine or copied game/tutorial code.
// This file deliberately has no Android dependencies so the rules can be tested on the JVM.
const val COLS = 7
const val ROWS = 8
const val KINDS = 6
const val NORMAL = 0
const val ROW_ROCKET = 1
const val COL_ROCKET = 2
const val RAINBOW = 3
const val BOMB = 4
const val START_HINTS = 3
const val START_HAMMERS = 1
const val START_SHUFFLES = 1
const val MOVE_BONUS = 50

data class Gem(val id: Long, val kind: Int, val special: Int = NORMAL)
data class MatchPlan(val clear: Set<Int>, val create: Map<Int, Int> = emptyMap(), val activatedRainbows: Set<Int> = emptySet())
data class Goal(val type: String, val count: Int, val kind: Int = -1)
data class Level(
    val number: Int, val title: String, val description: String,
    val moves: Int, val target: Int, val goals: List<Goal>,
    val ice: Set<Int> = emptySet(), val bossHp: Int = 0
)

private fun icePatch(count: Int, seed: Int): Set<Int> = (0 until COLS * ROWS)
    .map { (it * 13 + seed) % (COLS * ROWS) }.take(count).toSet()

// Ten authored missions, rather than one score challenge with increasing numbers.
// The score target is a star benchmark; only the listed goals decide victory.
val levels = listOf(
    Level(1, "A Foxy Welcome", "Find the foxes hiding along the meadow trail.", 20, 1800,
        listOf(Goal("collect", 12, 0))),
    Level(2, "Picnic Pals", "Bring both frogs and bunnies to the forest picnic.", 22, 2500,
        listOf(Goal("collect", 12, 1), Goal("collect", 12, 2))),
    Level(3, "Frozen Footpath", "Break every ice block to reopen the woodland path.", 22, 2800,
        listOf(Goal("ice", 12)), icePatch(12, 8)),
    Level(4, "Rocket Workshop", "Make special pieces and round up the little chicks.", 24, 3500,
        listOf(Goal("make", 4), Goal("collect", 14, 5))),
    Level(5, "Owl Post", "The owls need a team of blue bears for a moonlit delivery.", 23, 4000,
        listOf(Goal("collect", 20, 4), Goal("collect", 16, 3))),
    Level(6, "Thaw the Grove", "Free the frozen grove and guide the foxes home.", 25, 4400,
        listOf(Goal("ice", 18), Goal("collect", 18, 0)), icePatch(18, 4)),
    Level(7, "Chain Reaction", "Activate special pieces and set off six extra cascade waves.", 24, 4700,
        listOf(Goal("activate", 5), Goal("cascade", 6))),
    Level(8, "Rainbow Rescue", "Rescue ten of every kind of forest friend.", 24, 5200,
        List(KINDS) { Goal("collect", 10, it) }),
    Level(9, "The Last Seal", "Break the final seal with ice clearing, special attacks and bunnies.", 26, 6000,
        listOf(Goal("ice", 20), Goal("activate", 6), Goal("collect", 22, 2)), icePatch(20, 1)),
    Level(10, "The Thorn King", "Exploit the king's changing weakness and survive his freezing vines.", 28, 7200,
        listOf(Goal("boss", 240)), icePatch(8, 10), bossHp = 240)
)

fun dailyLevel(day: Long): Level {
    val ice = (0 until COLS * ROWS).filter { (it + day) % 4L == 0L }.toSet()
    return Level(0, "Daily forest", "A fresh combination of collection, ice and score goals.", 22, 4500,
        listOf(Goal("collect", 26, (day % KINDS).toInt()), Goal("ice", ice.size), Goal("score", 4500)), ice)
}

// Immutable data holders have no methods and are excluded from the class limit.
data class GameState(
    val board: List<Gem?> = emptyList(), val level: Level = levels[0],
    val mode: String = "story", val day: Long = 0,
    val moves: Int = level.moves, val score: Int = 0,
    val collections: List<Int> = List(KINDS) { 0 }, val specialsMade: Int = 0,
    val specialsUsed: Int = 0, val cascadeWaves: Int = 0,
    val ice: Set<Int> = emptySet(), val selected: Int = -1,
    val removing: Set<Int> = emptySet(), val hint: Set<Int> = emptySet(),
    val busy: Boolean = false, val paused: Boolean = false, val briefing: Boolean = false,
    val result: String = "", val combo: Int = 0, val bestCombo: Int = 0,
    val hints: Int = START_HINTS, val hammers: Int = START_HAMMERS,
    val shuffles: Int = START_SHUFFLES, val hammerMode: Boolean = false,
    val bossHealth: Int = level.bossHp, val bossWeakness: Int = 0, val bossTurns: Int = 0,
    val toast: String = "welcome", val toastPoints: Int = 0,
    val cue: String = "", val cueId: Int = 0
)

fun animalName(kind: Int): String = listOf("Fox", "Frog", "Bunny", "Bear", "Owl", "Chick")[kind]

fun goalLabel(goal: Goal): String = when (goal.type) {
    "collect" -> "Collect ${goal.count} ${animalName(goal.kind)} friends"
    "ice" -> "Break ${goal.count} ice blocks"
    "make" -> "Create ${goal.count} special pieces"
    "activate" -> "Activate ${goal.count} special pieces"
    "cascade" -> "Trigger ${goal.count} extra cascade waves"
    "score" -> "Reach ${goal.count} points"
    "boss" -> "Defeat the Thorn King"
    else -> error("Unknown mission: ${goal.type}")
}

fun goalProgress(s: GameState, goal: Goal): Int = when (goal.type) {
    "collect" -> s.collections.getOrElse(goal.kind) { 0 }
    "ice" -> s.level.ice.count { it !in s.ice }
    "make" -> s.specialsMade
    "activate" -> s.specialsUsed
    "cascade" -> s.cascadeWaves
    "score" -> s.score
    "boss" -> s.level.bossHp - s.bossHealth
    else -> 0
}

fun missionsComplete(s: GameState): Boolean = s.level.goals.isNotEmpty() &&
    s.level.goals.all { goalProgress(s, it) >= it.count }

// Pure rules shared by the ViewModel and JVM tests, including the boss battle.
fun recordWave(s: GameState, clear: Set<Int>, created: Map<Int, Int>, chain: Int): GameState {
    val melted = (clear + created.keys).intersect(s.ice)
    val used = clear.count { s.board[it]?.special?.let { special -> special != NORMAL } == true }
    val points = clear.size * 50 * chain.coerceAtMost(6) + melted.size * 40
    val damage = if (s.level.bossHp > 0) {
        // Frozen friends shatter their ice but cannot deal color damage on that clear.
        clear.count { it !in s.ice && s.board[it]?.kind == s.bossWeakness } * 4 + used * 10 + if (chain > 1) 6 else 0
    } else 0
    return s.copy(removing = clear, score = s.score + points,
        collections = List(KINDS) { kind -> s.collections[kind] + clear.count { s.board[it]?.kind == kind } },
        ice = s.ice - melted, specialsMade = s.specialsMade + created.size, specialsUsed = s.specialsUsed + used,
        cascadeWaves = s.cascadeWaves + if (chain > 1) 1 else 0,
        combo = chain, bestCombo = maxOf(s.bestCombo, chain), bossHealth = (s.bossHealth - damage).coerceAtLeast(0),
        toast = if (damage > 0) "boss_hit" else if (chain > 1) "combo" else "match",
        toastPoints = if (damage > 0) damage.coerceAtMost(s.bossHealth) else points)
}

fun bossAttackInterval(s: GameState): Int = if (s.level.bossHp > 0 && s.bossHealth * 2 <= s.level.bossHp) 2 else 3

fun finishTurn(s: GameState, spentMove: Boolean): GameState {
    if (missionsComplete(s)) {
        val bonus = s.moves * MOVE_BONUS
        return s.copy(busy = false, result = "won", score = s.score + bonus,
            toast = "win", toastPoints = bonus, selected = -1)
    }
    if (s.moves <= 0) return s.copy(busy = false, result = "lost", selected = -1)
    if (s.level.bossHp > 0 && spentMove) {
        val turns = s.bossTurns + 1
        if (turns >= bossAttackInterval(s)) {
            val count = if (bossAttackInterval(s) == 2) 3 else 2
            val frost = (0 until COLS * ROWS).map { (it * 13 + (s.level.moves - s.moves) * 11) % (COLS * ROWS) }
                .filter { it !in s.ice }.take(count)
            return s.copy(busy = false, ice = s.ice + frost, bossTurns = 0,
                bossWeakness = (s.bossWeakness + 1) % KINDS, toast = "boss_attack", toastPoints = 0)
        }
        return s.copy(busy = false, bossTurns = turns)
    }
    return s.copy(busy = false)
}

fun adjacent(a: Int, b: Int): Boolean = a in 0 until COLS * ROWS && b in 0 until COLS * ROWS &&
    abs(a / COLS - b / COLS) + abs(a % COLS - b % COLS) == 1

fun swapped(board: List<Gem?>, a: Int, b: Int): List<Gem?> = board.toMutableList().also {
    val temp = it[a]; it[a] = it[b]; it[b] = temp
}

class MatchEngine(private val random: Random = Random.Default) {
    private var nextId = 1L

    fun adopt(board: List<Gem?>) { nextId = (board.filterNotNull().maxOfOrNull { it.id } ?: 0) + 1 }

    fun fresh(): List<Gem?> {
        // Fill without initial triples, then reject a board if it has no legal move.
        repeat(500) {
            val board = mutableListOf<Gem?>()
            repeat(COLS * ROWS) { i ->
                val allowed = (0 until KINDS).filter { kind ->
                    !(i % COLS >= 2 && board[i - 1]?.kind == kind && board[i - 2]?.kind == kind) &&
                        !(i >= 2 * COLS && board[i - COLS]?.kind == kind && board[i - 2 * COLS]?.kind == kind)
                }
                board.add(Gem(nextId++, allowed.random(random)))
            }
            if (hint(board) != null) return board
        }
        error("Unable to create a playable board")
    }

    private fun runs(board: List<Gem?>): List<List<Int>> {
        val result = mutableListOf<List<Int>>()
        fun scan(line: List<Int>) {
            var start = 0
            while (start < line.size) {
                val kind = board[line[start]]?.kind
                var end = start + 1
                while (kind != null && end < line.size && board[line[end]]?.kind == kind) end++
                if (kind != null && end - start >= 3) result.add(line.subList(start, end).toList())
                start = end
            }
        }
        repeat(ROWS) { r -> scan(List(COLS) { c -> r * COLS + c }) }
        repeat(COLS) { c -> scan(List(ROWS) { r -> r * COLS + c }) }
        return result
    }

    fun matches(board: List<Gem?>): Set<Int> = runs(board).flatten().toSet()

    fun legal(board: List<Gem?>, a: Int, b: Int): Boolean {
        if (!adjacent(a, b)) return false
        val x = board[a] ?: return false
        val y = board[b] ?: return false
        if (x.special == RAINBOW || y.special == RAINBOW ||
            (x.special != NORMAL && y.special != NORMAL)) return true
        val hit = matches(swapped(board, a, b))
        return a in hit || b in hit
    }

    fun hint(board: List<Gem?>): Pair<Int, Int>? {
        for (a in board.indices) {
            if (a % COLS < COLS - 1 && legal(board, a, a + 1)) return a to a + 1
            if (a / COLS < ROWS - 1 && legal(board, a, a + COLS)) return a to a + COLS
        }
        return null
    }

    fun plan(board: List<Gem?>, preferred: List<Int> = emptyList()): MatchPlan {
        val groups = runs(board).toMutableList()
        val all = groups.flatten().toSet()
        val create = mutableMapOf<Int, Int>()
        // Merge intersecting runs: a T or L creates exactly one area bomb.
        while (groups.isNotEmpty()) {
            val component = mutableListOf(groups.removeAt(0))
            var joined: Boolean
            do {
                val cells = component.flatten().toSet()
                val touching = groups.filter { line -> line.any { it in cells } }
                joined = touching.isNotEmpty()
                component.addAll(touching)
                groups.removeAll(touching.toSet())
            } while (joined)
            val cells = component.flatten().distinct()
            val longest = component.maxBy { it.size }
            val special = when {
                longest.size >= 5 -> RAINBOW
                component.size > 1 -> BOMB
                longest.size == 4 -> if (longest[0] / COLS == longest[1] / COLS) ROW_ROCKET else COL_ROCKET
                else -> NORMAL
            }
            if (special != NORMAL) {
                val available = cells.filter { board[it]?.special == NORMAL }
                val at = preferred.firstOrNull { it in available }
                    ?: available.getOrNull(available.size / 2)
                if (at != null) create[at] = special
            }
        }
        return MatchPlan(all, create)
    }

    fun swapEffect(board: List<Gem?>, a: Int, b: Int): Set<Int>? {
        val x = board[a] ?: return null
        val y = board[b] ?: return null
        if (x.special == RAINBOW && y.special == RAINBOW) return board.indices.toSet()
        if (x.special == RAINBOW || y.special == RAINBOW) {
            val kind = if (x.special == RAINBOW) y.kind else x.kind
            return board.indices.filter { board[it]?.kind == kind }.toSet() + a + b
        }
        if (x.special != NORMAL && y.special != NORMAL) return setOf(a, b)
        return null
    }

    fun expand(board: List<Gem?>, seed: Set<Int>, protected: Set<Int> = emptySet(), activatedRainbows: Set<Int> = emptySet()): Set<Int> {
        val clear = seed.toMutableSet().apply { removeAll(protected) }
        val queue = ArrayDeque(clear)
        val activated = mutableSetOf<Int>()
        while (queue.isNotEmpty()) {
            val at = queue.removeFirst()
            if (!activated.add(at)) continue
            val gem = board[at] ?: continue
            val r = at / COLS; val c = at % COLS
            val extra = when (if (at in activatedRainbows) NORMAL else gem.special) {
                ROW_ROCKET -> List(COLS) { r * COLS + it }
                COL_ROCKET -> List(ROWS) { it * COLS + c }
                BOMB -> board.indices.filter { abs(it / COLS - r) <= 1 && abs(it % COLS - c) <= 1 }
                RAINBOW -> {
                    val most = board.filterNotNull().groupingBy { it.kind }.eachCount().maxByOrNull { it.value }?.key
                    board.indices.filter { board[it]?.kind == most }
                }
                else -> emptyList()
            }
            extra.filter { it !in protected && clear.add(it) }.forEach { queue.addLast(it) }
        }
        return clear
    }

    fun fall(board: List<Gem?>): List<Gem?> {
        val result = board.toMutableList()
        repeat(COLS) { c ->
            val kept = (ROWS - 1 downTo 0).mapNotNull { board[it * COLS + c] }
            for (r in ROWS - 1 downTo 0) {
                result[r * COLS + c] = kept.getOrNull(ROWS - 1 - r) ?: Gem(nextId++, random.nextInt(KINDS))
            }
        }
        return result
    }

    fun shuffle(board: List<Gem?>): List<Gem?> {
        repeat(300) {
            val shuffled = board.shuffled(random)
            if (matches(shuffled).isEmpty() && hint(shuffled) != null) return shuffled
        }
        return fresh()
    }
}
