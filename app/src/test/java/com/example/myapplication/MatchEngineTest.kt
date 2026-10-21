package com.example.myapplication

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class MatchEngineTest {
    private fun patterned(): List<Gem?> = List(COLS * ROWS) { i -> Gem(i.toLong() + 1, (i / COLS * 2 + i % COLS) % KINDS) }
    private fun withKinds(vararg cells: Pair<Int, Int>): List<Gem?> = patterned().toMutableList().also { b ->
        cells.forEach { (at, kind) -> b[at] = b[at]!!.copy(kind = kind) }
    }

    @Test fun freshBoardsHaveNoTriplesAndAlwaysHaveLegalMoves() {
        repeat(200) { seed ->
            val e = MatchEngine(Random(seed)); val b = e.fresh()
            assertEquals(56, b.size)
            assertEquals(56, b.filterNotNull().map { it.id }.toSet().size)
            assertTrue(e.matches(b).isEmpty())
            val (a, c) = requireNotNull(e.hint(b))
            assertTrue(e.legal(b, a, c))
        }
    }

    @Test fun adjacencyDoesNotWrapBetweenRows() {
        assertFalse(adjacent(6, 7)); assertFalse(adjacent(-1, 0)); assertFalse(adjacent(55, 56))
        assertTrue(adjacent(6, 13)); assertTrue(adjacent(0, 1))
        assertFalse(MatchEngine().legal(patterned(), 0, 2))
    }

    @Test fun horizontalAndVerticalRunsAreBothFound() {
        val e = MatchEngine()
        val b = withKinds(0 to 5, 1 to 5, 2 to 5, 20 to 3, 27 to 3, 34 to 3)
        assertTrue(e.matches(b).containsAll(setOf(0, 1, 2, 20, 27, 34)))
    }

    @Test fun fourCreatesRocketAtTheSwappedDestination() {
        val e = MatchEngine()
        val b = withKinds(14 to 5, 15 to 5, 16 to 5, 17 to 5)
        assertEquals(ROW_ROCKET, e.plan(b, listOf(15)).create[15])
        val v = withKinds(1 to 5, 8 to 5, 15 to 5, 22 to 5)
        assertEquals(COL_ROCKET, e.plan(v, listOf(15)).create[15])
    }

    @Test fun fiveCreatesRainbowAndTShapeCreatesOneBomb() {
        val e = MatchEngine()
        val five = withKinds(*(14..18).map { it to 5 }.toTypedArray())
        assertEquals(RAINBOW, e.plan(five, listOf(16)).create[16])
        val t = withKinds(15 to 5, 16 to 5, 17 to 5, 9 to 5, 23 to 5)
        val plan = e.plan(t, listOf(16))
        assertEquals(1, plan.create.size)
        assertEquals(BOMB, plan.create[16])
    }

    @Test fun specialChainExpandsAndNewSpecialIsProtected() {
        val b = patterned().toMutableList()
        b[16] = b[16]!!.copy(special = ROW_ROCKET)
        b[18] = b[18]!!.copy(special = COL_ROCKET)
        val clear = MatchEngine().expand(b, setOf(16), setOf(15))
        assertFalse(15 in clear)
        assertTrue((14..20).filter { it != 15 }.all { it in clear })
        assertTrue((0 until ROWS).all { it * COLS + 4 in clear })
    }

    @Test fun rainbowSwapClearsOnlyTheChosenColorAndBothSwapCells() {
        val e = MatchEngine(); val b = patterned().toMutableList()
        b[0] = b[0]!!.copy(special = RAINBOW)
        val after = swapped(b, 0, 1)
        val seed = requireNotNull(e.swapEffect(after, 0, 1))
        val actual = e.expand(after, seed, activatedRainbows = setOf(1))
        val expected = after.indices.filter { after[it]?.kind == b[1]?.kind }.toSet() + 0 + 1
        assertEquals(expected, actual)
        assertTrue(e.legal(b, 0, 1))
    }

    @Test fun fallPreservesColumnOrderAndAssignsUniqueIds() {
        val e = MatchEngine(Random(8)); val b = e.fresh().toMutableList()
        val keep = b[7]!!.id; val bottom = b[49]!!.id
        b[0] = null; b[14] = null; b[35] = null
        val fallen = e.fall(b)
        assertEquals(56, fallen.filterNotNull().size)
        assertEquals(56, fallen.filterNotNull().map { it.id }.toSet().size)
        assertEquals(bottom, fallen[49]!!.id)
        assertEquals(keep, fallen[21]!!.id)
    }

    @Test fun shuffledBoardsRemainPlayableWithoutFreeMatches() {
        repeat(50) { seed ->
            val e = MatchEngine(Random(seed)); val b = e.fresh()
            val mixed = e.shuffle(b)
            assertTrue(e.matches(mixed).isEmpty()); assertNotNull(e.hint(mixed))
            assertEquals(b.filterNotNull().map { it.id }.toSet(), mixed.filterNotNull().map { it.id }.toSet())
        }
    }

    @Test fun seededDailyOpeningIsRepeatable() {
        assertEquals(MatchEngine(Random(2026294)).fresh(), MatchEngine(Random(2026294)).fresh())
    }

    @Test fun hundredsOfTurnsMaintainStablePlayableBoards() {
        repeat(15) { seed ->
            val e = MatchEngine(Random(seed)); var b = e.fresh()
            repeat(35) {
                val (a, c) = requireNotNull(e.hint(b))
                b = swapped(b, a, c)
                val effect = e.swapEffect(b, a, c)
                var plan = effect?.let { MatchPlan(it, activatedRainbows = setOf(a, c).filter { p -> b[p]?.special == RAINBOW }.toSet()) }
                    ?: e.plan(b, listOf(c, a))
                var waves = 0
                while (plan.clear.isNotEmpty() && waves++ < 40) {
                    val clear = e.expand(b, plan.clear, plan.create.keys, plan.activatedRainbows)
                    b = e.fall(b.mapIndexed { p, gem -> when {
                        p in plan.create -> gem?.copy(special = plan.create.getValue(p))
                        p in clear -> null
                        else -> gem
                    } })
                    plan = e.plan(b)
                }
                assertTrue("Cascade failed to settle", waves < 40)
                if (e.hint(b) == null) b = e.shuffle(b)
                assertTrue(e.matches(b).isEmpty()); assertNotNull(e.hint(b))
                assertEquals(56, b.filterNotNull().map { it.id }.toSet().size)
            }
        }
    }

    @Test fun campaignHasTenDistinctMissionsAndOnlyTheFinaleIsABoss() {
        assertEquals((1..10).toList(), levels.map { it.number })
        assertEquals(10, levels.map { it.goals }.distinct().size)
        assertTrue(levels.dropLast(1).all { it.bossHp == 0 })
        assertEquals(240, levels.last().bossHp)
        assertEquals(List(KINDS) { it }, levels[7].goals.map { it.kind })
    }

    @Test fun eachListedColorIsRequiredButUnlistedScoreIsNot() {
        val level = levels[1]
        val partial = GameState(level = level, score = 99999, collections = listOf(0, 12, 11, 0, 0, 0))
        assertFalse(missionsComplete(partial))
        val complete = partial.copy(score = 0, collections = listOf(0, 12, 12, 0, 0, 0), moves = 0)
        assertTrue(missionsComplete(complete))
        assertEquals("won", finishTurn(complete, true).result)
    }

    @Test fun iceMissionWinsOnlyAfterEveryRequestedBlockBreaks() {
        val level = levels[2]
        val lastIce = GameState(level = level, ice = setOf(level.ice.first()), score = 99999)
        assertEquals(11, goalProgress(lastIce, level.goals.single()))
        assertFalse(missionsComplete(lastIce))
        assertTrue(missionsComplete(lastIce.copy(ice = emptySet(), score = 0)))
    }

    @Test fun wavesCountCollectionsCreationsActivationsAndExtraCascadesSeparately() {
        val board = patterned().toMutableList()
        board[0] = board[0]!!.copy(special = ROW_ROCKET)
        val s = GameState(board = board, level = levels[3])
        val first = recordWave(s, setOf(0, 1, 2), mapOf(3 to BOMB), 1)
        assertEquals(1, first.specialsMade)
        assertEquals(1, first.specialsUsed)
        assertEquals(0, first.cascadeWaves)
        assertEquals(3, first.collections.sum())
        assertEquals(1, recordWave(first, setOf(4, 5, 6), emptyMap(), 2).cascadeWaves)
    }

    @Test fun frozenWeakColorDoesNotDamageBossButSpecialsAndCascadesDo() {
        val board = patterned().toMutableList()
        board[0] = board[0]!!.copy(kind = 0)
        board[1] = board[1]!!.copy(kind = 0)
        board[2] = board[2]!!.copy(kind = 1, special = ROW_ROCKET)
        val s = GameState(board = board, level = levels.last(), ice = setOf(1), bossWeakness = 0)
        val after = recordWave(s, setOf(0, 1, 2, 3), emptyMap(), 2)
        assertEquals(220, after.bossHealth) // 4 color + 10 special + 6 cascade.
        assertTrue(after.ice.isEmpty())
        assertEquals(2, after.collections[0])
        val immune = recordWave(s, setOf(3, 4, 5), emptyMap(), 1)
        assertEquals(240, immune.bossHealth)
    }

    @Test fun bossCounterattackFreezesTilesAndRotatesWeaknessAfterThreeMoves() {
        val s = GameState(level = levels.last(), moves = 25, ice = levels.last().ice, bossTurns = 2)
        val after = finishTurn(s, true)
        assertEquals(s.ice.size + 2, after.ice.size)
        assertEquals(1, after.bossWeakness)
        assertEquals(0, after.bossTurns)
        assertEquals(s.ice, finishTurn(s, false).ice) // Free tools do not advance the boss clock.
    }

    @Test fun enragedBossAttacksFasterAndFreezesThreeTiles() {
        val s = GameState(level = levels.last(), moves = 20, ice = levels.last().ice, bossHealth = 120, bossTurns = 1)
        assertEquals(2, bossAttackInterval(s))
        assertEquals(s.ice.size + 3, finishTurn(s, true).ice.size)
        assertEquals(s.ice, finishTurn(s.copy(bossHealth = 121), true).ice)
    }

    @Test fun defeatedBossWinsBeforeLastMoveLossAndCannotRetaliate() {
        val s = GameState(level = levels.last(), moves = 0, bossHealth = 0, bossTurns = 2, ice = (0 until 56).toSet())
        val result = finishTurn(s, true)
        assertEquals("won", result.result)
        assertEquals(s.ice, result.ice)
        assertEquals(0, result.bossWeakness)
        assertEquals("lost", finishTurn(s.copy(bossHealth = 1, score = 99999), true).result)
    }

    @Test fun authoredMissionsHaveWinningPathsWithAPlanningPlayer() {
        // Deterministic playtesting uses the actual production rules, without refill look-ahead.
        levels.forEach { level ->
            val won = (0 until 12).any { seed -> playMission(level, seed + level.number * 100) }
            assertTrue("No winning playtest path for stage ${level.number}: ${level.title}", won)
        }
    }

    private fun playMission(level: Level, seed: Int): Boolean {
        val e = MatchEngine(Random(seed))
        var s = GameState(level = level, board = e.fresh(), moves = level.moves, ice = level.ice)
        while (s.result.isEmpty()) {
            val hammer = when {
                s.hammers <= 0 -> null
                level.goals.any { it.type == "ice" } && s.ice.size == 1 -> s.ice.first()
                level.bossHp > 0 && s.bossHealth <= 4 -> s.board.indices.firstOrNull { it !in s.ice && s.board[it]?.kind == s.bossWeakness }
                else -> null
            }
            if (hammer != null) {
                s = settleForTest(s.copy(hammers = 0), e, MatchPlan(setOf(hammer)), false)
                continue
            }
            val choices = s.board.indices.flatMap { a -> listOf(a + 1, a + COLS).filter { b -> e.legal(s.board, a, b) }.map { a to it } }
            val (a, b) = choices.maxBy { (x, y) ->
                val after = swapped(s.board, x, y)
                val plan = e.plan(after, listOf(y, x))
                val clear = e.expand(after, e.swapEffect(after, x, y) ?: plan.clear, plan.create.keys)
                val next = recordWave(s.copy(board = after), clear, plan.create, 1)
                level.goals.sumOf { goal ->
                    val delta = (goalProgress(next, goal).coerceAtMost(goal.count) - goalProgress(s, goal).coerceAtMost(goal.count))
                    delta * when (goal.type) { "ice" -> 8; "make", "activate" -> 18; "boss" -> 4; else -> 4 }
                } + clear.size * .1 + plan.create.size * 6
            }
            s = s.copy(board = swapped(s.board, a, b), moves = s.moves - 1)
            val effect = e.swapEffect(s.board, a, b)
            val plan = effect?.let { MatchPlan(it, activatedRainbows = setOf(a, b).filter { p -> s.board[p]?.special == RAINBOW }.toSet()) }
                ?: e.plan(s.board, listOf(b, a))
            s = settleForTest(s, e, plan, true)
        }
        return s.result == "won"
    }

    private fun settleForTest(initial: GameState, e: MatchEngine, first: MatchPlan, spent: Boolean): GameState {
        var s = initial; var plan = first; var wave = 0
        while (plan.clear.isNotEmpty() && wave++ < 40) {
            val clear = e.expand(s.board, plan.clear, plan.create.keys, plan.activatedRainbows)
            s = recordWave(s, clear, plan.create, wave)
            val holes = s.board.mapIndexed { at, gem -> when {
                at in plan.create -> gem?.copy(special = plan.create.getValue(at))
                at in clear -> null
                else -> gem
            } }
            s = s.copy(board = e.fall(holes), removing = emptySet())
            plan = e.plan(s.board)
        }
        s = finishTurn(s, spent)
        if (s.result.isEmpty() && e.hint(s.board) == null) s = s.copy(board = e.shuffle(s.board))
        return s
    }
}
