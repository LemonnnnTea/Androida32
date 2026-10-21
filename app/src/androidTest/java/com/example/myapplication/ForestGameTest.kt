package com.example.myapplication

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class ForestGameTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val vm get() = ViewModelProvider(ui.activity)[GameViewModel::class.java]

    private fun start(daily: Boolean = false) {
        ui.runOnIdle { vm.start(0, daily) }
        ui.onNodeWithTag("begin_level").performClick()
        ui.onNodeWithTag("board").assertIsDisplayed()
    }

    private fun settled() { ui.waitUntil(20_000) { !vm.state.busy } }

    @Test fun tapsMakeARealMatchAndSaveCanBeResumed() {
        start()
        val original = vm.state
        val (a, b) = requireNotNull(MatchEngine().hint(original.board))
        ui.onNodeWithTag("cell_$a").performClick()
        ui.onNodeWithTag("cell_$b").performClick()
        settled()
        assertEquals(original.moves - 1, vm.state.moves)
        assertTrue(vm.state.score > 0)
        assertEquals(56, vm.state.board.filterNotNull().size)
        if (vm.state.result.isEmpty()) {
            val saved = vm.state
            ui.onNodeWithTag("pause").performClick()
            ui.onNodeWithTag("save_home").performClick()
            ui.onNodeWithTag("continue").performClick()
            assertEquals(saved.score, vm.state.score)
            assertEquals(saved.board, vm.state.board)
            assertFalse(vm.state.paused)
        }
    }

    @Test fun invalidSwapDoesNotSpendAMoveOrChangeTheBoard() {
        start()
        val original = vm.state
        val e = MatchEngine()
        val a = (0 until 55).first { it % COLS < COLS - 1 && !e.legal(original.board, it, it + 1) }
        ui.onNodeWithTag("cell_$a").performClick()
        ui.onNodeWithTag("cell_${a + 1}").performClick()
        settled()
        assertEquals(original.moves, vm.state.moves)
        assertEquals(original.board, vm.state.board)
        assertEquals(0, vm.state.score)
    }

    @Test fun swipeGestureMakesALegalMatch() {
        start()
        val original = vm.state
        val (a, b) = requireNotNull(MatchEngine().hint(original.board))
        ui.onNodeWithTag("cell_$a").performTouchInput {
            val distance = width * .75f
            val delta = if (b == a + 1) Offset(distance, 0f) else Offset(0f, distance)
            swipe(center, center + delta, 250)
        }
        settled()
        assertEquals(original.moves - 1, vm.state.moves)
        assertTrue(vm.state.score > 0)
    }

    @Test fun toolsAreFreeAndHammerInventoryIsBounded() {
        start()
        val moves = vm.state.moves
        ui.onNodeWithTag("tool_hint").performClick()
        assertEquals(2, vm.state.hint.size)
        ui.onNodeWithTag("tool_shuffle").performClick()
        settled()
        assertEquals(0, vm.state.shuffles)
        assertEquals(moves, vm.state.moves)
        ui.onNodeWithTag("tool_hammer").performClick()
        ui.onNodeWithTag("cell_20").performClick()
        settled()
        assertEquals(0, vm.state.hammers)
        assertEquals(moves, vm.state.moves)
        assertTrue(vm.state.score >= 50)
        ui.onNodeWithTag("tool_shuffle").assertIsNotEnabled()
        ui.onNodeWithTag("tool_hammer").assertIsNotEnabled()
    }

    @Test fun recreationRetainsGameAndAutomaticallyPauses() {
        start()
        ui.onNodeWithTag("tool_shuffle").performClick()
        settled()
        val saved = vm.state
        ui.activityRule.scenario.recreate()
        ui.waitForIdle()
        assertEquals(saved.board, vm.state.board)
        assertEquals(saved.shuffles, vm.state.shuffles)
        assertTrue(vm.state.paused)
        ui.onNodeWithTag("resume").performClick()
        ui.onNodeWithTag("board").assertIsDisplayed()
    }

    @Test fun dailyOpeningRepeatsAndLandscapeCanStillPlay() {
        start(daily = true)
        val board = vm.state.board
        ui.runOnIdle { vm.start(daily = true) }
        assertEquals(board, vm.state.board)
        ui.onNodeWithTag("begin_level").performClick()
        ui.runOnIdle { ui.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        ui.waitForIdle()
        if (vm.state.paused) ui.onNodeWithTag("resume").performClick()
        ui.onNodeWithTag("board").assertIsDisplayed()
        ui.runOnIdle { ui.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    @Test fun completingTheMissionUnlocksTheNextStageAndShowsItsBriefing() {
        // Random boards may legitimately be lost. A fixed near-complete checkpoint
        // makes this a repeatable progression test instead of assuming every run wins.
        val e = MatchEngine(Random(417))
        val (board, move) = generateSequence { e.fresh() }.mapNotNull { board ->
            val pair = board.indices.flatMap { a -> listOf(a + 1, a + COLS)
                .filter { b -> e.legal(board, a, b) }.map { a to it } }
                .firstOrNull { (a, b) ->
                    val after = swapped(board, a, b)
                    e.matches(after).any { after[it]?.kind == 0 }
                }
            pair?.let { board to it }
        }.first()
        val save = saveFixture(board, version = 3, moves = 10, ice = emptySet())
            .put("collections", JSONArray(listOf(11, 0, 0, 0, 0, 0)))
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        ui.activityRule.scenario.onActivity { activity ->
            activity.viewModelStore.clear()
            activity.getSharedPreferences("forest_pop_v1", 0).edit()
                .putString("run", save.toString()).putInt("campaign3_stars_0", 0).apply()
        }
        ui.activityRule.scenario.recreate()
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        ui.onNodeWithTag("continue").performClick()
        ui.onNodeWithTag("cell_${move.first}").performClick()
        ui.onNodeWithTag("cell_${move.second}").performClick()
        settled()
        assertEquals("won", vm.state.result)
        assertTrue(vm.stars[0] > 0)
        assertTrue(vm.unlocked() >= 1)
        assertFalse(vm.hasSave)
        ui.onNodeWithTag("result_action").performClick()
        assertEquals(2, vm.state.level.number)
        assertTrue(vm.state.briefing)
        ui.onNodeWithText("Collect 12 Frog friends").assertIsDisplayed()
        ui.onNodeWithText("Collect 12 Bunny friends").assertIsDisplayed()
    }

    @Test fun interfaceIsEnglishAndHasNoLanguagePicker() {
        ui.runOnIdle {
            ui.activity.getSharedPreferences("forest_pop_v1", 0).edit().putBoolean("english", false).apply()
            vm.go("home")
        }
        assertEquals("en", ui.activity.resources.configuration.locales[0].language)
        assertEquals("Forest Pop", ui.activity.getString(R.string.app_name))
        ui.onNodeWithText("Forest Pop").assertIsDisplayed()
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithText("Make it yours").assertIsDisplayed()
        ui.onNodeWithTag("language_zh").assertDoesNotExist()
        ui.onNodeWithTag("language_en").assertDoesNotExist()
        ui.onNodeWithContentDescription("Close").performClick()
        ui.onNodeWithTag("help").performClick()
        ui.onNodeWithText("A tiny field guide").assertIsDisplayed()
    }

    @Test fun hintQuotaCannotBeExceededAndSurvivesReload() {
        start()
        repeat(START_HINTS) {
            ui.onNodeWithTag("tool_hint").performClick()
            assertEquals(START_HINTS - it - 1, vm.state.hints)
            // Deselect the hint by selecting just one tile, without spending a move.
            ui.onNodeWithTag("cell_0").performClick()
            ui.onNodeWithTag("cell_0").performClick()
        }
        ui.onNodeWithTag("tool_hint").assertIsNotEnabled()
        ui.runOnIdle {
            vm.hint()
            assertEquals(0, vm.state.hints)
            val restored = GameViewModel(ui.activity.application)
            assertEquals(0, restored.state.hints)
            assertEquals(levels[0].moves, restored.state.moves)
            assertEquals(vm.state.board, restored.state.board)
        }
    }

    @Test fun oldSaveUpgradesToHarderRulesWithoutLosingBoardOrStars() {
        val day = today()
        val board = MatchEngine(Random(100)).fresh()
        val ice = (0 until COLS * ROWS).filter { (it + day) % 5L == 0L }.toSet()
        val broken = ice.first()
        ui.runOnIdle {
            val save = saveFixture(board, version = 1, moves = 28, ice = ice - broken)
                .put("mode", "daily").put("level", 0).put("day", day)
            val prefs = ui.activity.getSharedPreferences("forest_pop_v1", 0)
            prefs.edit().putString("run", save.toString()).remove("campaign3_stars_0").putInt("stars_0", 2).apply()
            val restored = GameViewModel(ui.activity.application)
            assertEquals(board, restored.state.board)
            assertEquals(500, restored.state.score)
            assertEquals(5, restored.state.collections[(day % KINDS).toInt()])
            assertEquals(22, restored.state.moves)
            assertEquals(1, restored.state.hammers)
            assertEquals(1, restored.state.shuffles)
            assertEquals(3, restored.state.hints)
            assertEquals(dailyLevel(day).ice - broken, restored.state.ice)
            assertEquals(2, restored.stars[0])
            assertEquals(3, JSONObject(prefs.getString("run", "")!!).getInt("version"))
        }
    }

    @Test fun runningOutOfMovesWithUnfinishedGoalsLosesTheLevel() {
        val board = MatchEngine(Random(200)).fresh()
        lateinit var game: GameViewModel
        ui.runOnIdle {
            val save = saveFixture(board, version = 2, moves = 1, ice = levels.last().ice).put("level", 10)
                .put("score", 0).put("collected", 0)
            ui.activity.getSharedPreferences("forest_pop_v1", 0).edit().putString("run", save.toString()).apply()
            game = GameViewModel(ui.activity.application)
            game.resume()
            val (a, b) = requireNotNull(MatchEngine().hint(board))
            game.swap(a, b)
        }
        ui.waitUntil(20_000) { !game.state.busy }
        assertEquals("lost", game.state.result)
        assertEquals(0, game.state.moves)
        assertFalse(game.hasSave)
    }

    private fun saveFixture(board: List<Gem?>, version: Int, moves: Int, ice: Set<Int>): JSONObject =
        JSONObject().put("version", version).put("level", 1).put("mode", "story").put("day", today())
            .put("moves", moves).put("score", 500).put("collected", 5).put("hammers", 2)
            .put("shuffles", 2).put("hints", START_HINTS).put("ice", JSONArray(ice.toList()))
            .put("board", JSONArray(board.map { g -> requireNotNull(g)
                JSONObject().put("id", g.id).put("kind", g.kind).put("special", g.special) }))

    @Test fun missionBriefingPreventsMovesUntilThePlayerBegins() {
        ui.runOnIdle { vm.start(0) }
        ui.onNodeWithText("Collect 12 Fox friends").assertIsDisplayed()
        val before = vm.state
        ui.runOnIdle {
            val (a, b) = requireNotNull(MatchEngine().hint(before.board))
            vm.swap(a, b)
            assertEquals(before.board, vm.state.board)
            assertEquals(before.moves, vm.state.moves)
        }
        ui.onNodeWithTag("begin_level").performClick()
        assertFalse(vm.state.briefing)
        ui.onNodeWithTag("goal_collect_0").assertIsDisplayed()
    }

    @Test fun trailContainsTenStagesWithALockedBossFinale() {
        ui.runOnIdle { vm.go("map") }
        ui.onNodeWithTag("level_10").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        ui.onNodeWithText("The Thorn King").assertIsDisplayed()
        ui.onNodeWithTag("level_11").assertDoesNotExist()
    }

    @Test fun bossHealthWeaknessAttackClockAndMissionCountersSurviveReload() {
        val board = MatchEngine(Random(301)).fresh()
        ui.runOnIdle {
            val save = saveFixture(board, version = 3, moves = 18, ice = levels.last().ice)
                .put("level", 10).put("collections", JSONArray(listOf(5, 7, 9, 2, 3, 4)))
                .put("bossHealth", 107).put("bossWeakness", 3).put("bossTurns", 1)
                .put("specialsMade", 4).put("specialsUsed", 2).put("cascadeWaves", 5)
            val prefs = ui.activity.getSharedPreferences("forest_pop_v1", 0)
            prefs.edit().putString("run", save.toString()).putInt("stars_9", 3).remove("campaign3_stars_9").apply()
            val restored = GameViewModel(ui.activity.application)
            assertEquals(107, restored.state.bossHealth)
            assertEquals(3, restored.state.bossWeakness)
            assertEquals(1, restored.state.bossTurns)
            assertEquals(4, restored.state.specialsMade)
            assertEquals(2, restored.state.specialsUsed)
            assertEquals(5, restored.state.cascadeWaves)
            assertEquals(listOf(5, 7, 9, 2, 3, 4), restored.state.collections)
            assertEquals(0, restored.stars[9]) // An old stage-10 score cannot bypass the new boss.
            restored.hint()
            val again = GameViewModel(ui.activity.application)
            assertEquals(restored.state.bossHealth, again.state.bossHealth)
            assertEquals(restored.state.bossTurns, again.state.bossTurns)
            assertEquals(restored.state.bossWeakness, again.state.bossWeakness)
            assertEquals(restored.state.collections, again.state.collections)
        }
    }
}
