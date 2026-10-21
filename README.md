# Forest Pop 1.2

An original, English-only match-three Android game with a **10-stage mission campaign** and a final boss battle, created for 159.336 Assignment 3.

## Run

Open this project in Android Studio, sync Gradle, select a device or emulator, and run `app`. Android 7.0 / API 24 or later is required. All screens, mission text, settings, application labels, and accessibility descriptions are English. There is no language picker.

## The campaign

Finish every listed mission within the move limit to unlock the next stage. Each stage opens with a mission briefing. Score is used for stars; it is a victory requirement only when an explicit score mission is listed.

| Stage | Title | Mission | Moves |
| --- | --- | --- | --- |
| 1 | A Foxy Welcome | Collect 12 Fox friends | 20 |
| 2 | Picnic Pals | Collect 12 Frogs and 12 Bunnies | 22 |
| 3 | Frozen Footpath | Break all 12 ice blocks | 22 |
| 4 | Rocket Workshop | Create 4 special pieces and collect 14 Chicks | 24 |
| 5 | Owl Post | Collect 20 Owls and 16 Bears | 23 |
| 6 | Thaw the Grove | Break 18 ice blocks and collect 18 Foxes | 25 |
| 7 | Chain Reaction | Activate 5 special pieces and trigger 6 extra cascade waves | 24 |
| 8 | Rainbow Rescue | Collect 10 of each of the six animal types | 24 |
| 9 | The Last Seal | Break 20 ice blocks, activate 6 specials, and collect 22 Bunnies | 26 |
| 10 | The Thorn King | Defeat the 240 HP boss | 28 |

The trail has 30 stars in total. Clearing a stage earns at least one star. Final scores at 1.4 / 2 times the stage's score benchmark earn two / three stars. Each unused move gives 50 bonus points.

## Boss: The Thorn King

The finale has its own original character art, health bar, weakness indicator, attack countdown, combat messages, and victory screen.

- Each unfrozen friend matching the current weakness deals **4 damage** when cleared.
- Each activated special piece adds **10 damage**. Newly created specials only count when they are later activated.
- Each extra cascade wave adds **6 damage**. The initial match is not an extra cascade.
- Frozen friends break their ice but deal no color damage on that clear. Specials and cascade bonuses still hit the boss.
- Above half HP, the king freezes **2 new cells after every 3 valid moves**.
- At 120 HP or less, he becomes enraged and freezes **3 cells after every 2 valid moves**.
- Every frost attack rotates the weakness to the next animal color. Free tools and invalid swaps do not advance his attack clock.
- Defeating the boss on the last move counts as victory and prevents his counterattack. Clearing all ice is not required in the boss stage.

## Matching and tools

Swipe two neighbors, or tap them in sequence. Match at least three of the same animal in a row or column. Invalid swaps bounce back without spending a move. Cascades multiply score up to six times.

Four in a line creates a directional rocket, five creates a rainbow, and a T/L creates a 3x3 bomb. Match a special to trigger it, or swap two specials. Swap a rainbow with a friend to clear that color.

Each game has **3 hints, 1 pop, and 1 mix**. Tools do not spend moves; spent charges are saved. A board without a legal move mixes automatically for free.

The optional daily mission remains separate from the campaign: 22 moves, 26 requested friends, 14 ice blocks, and 4,500 points. The opening board is fixed for the local date.

## Saving and upgrading

There is one active save slot; starting a new game replaces it. Stable boards, all six animal collection counts, special-creation/activation counters, cascade totals, ice, score, moves, tools, boss HP, weakness, and attack clock are saved in version-3 JSON. Completed stars, best scores, audio, and haptic settings persist independently.

Older version-1/2 saves retain their board, score, known collection data and any overlapping broken ice. Their stage is mapped into the new ten-stage campaign, and remaining moves/tools are capped at the new stage's limits. Previously earned stars for the first nine stages are preserved. The new boss must actually be defeated; an old ordinary stage-10 score cannot mark it complete. Older saves for stages 11/12 continue at the new finale.

Leaving the app pauses interaction and releases SoundPool. A bounded in-progress cascade may settle and checkpoint while paused. Only stable boards are written; killing the process during an animation restores the previous completed turn. ViewModel survives rotation, while private preferences restore state after process recreation. No network or runtime permissions are needed.

## Code and assignment requirements

This project was implemented with Codex assistance for this request. Gameplay, Compose UI, characters, launcher art and synthesized sounds were created for this project. No complete game, tutorial app, or game engine was copied or embedded.

There are **four production classes with behavior**: MainActivity, GameAudio, GameViewModel and MatchEngine. Gem, MatchPlan, Goal, Level and GameState are simple data holders with no methods. Two test classes are in separate test source sets. All application code is Kotlin and all interface elements use Compose. There are no XML View layouts, WebViews, or game engines.

| File | Responsibility |
| --- | --- |
| MainActivity.kt | English configuration, Compose entry point, SoundPool lifecycle |
| GameViewModel.kt | Turn orchestration, observable state, progression and save migration |
| MatchEngine.kt | Pure Kotlin board logic, mission definitions, objective progress and boss rules |
| ForestApp.kt | Trail, briefings, responsive game UI, mission counters, boss HUD and dialogs |
| ForestArt.kt | Original vector animals, Thorn King, interface icons and background |

The design uses the course's Kotlin collections, Compose state/ViewModel, coroutines, gestures, animations, Canvas, dialogs, preferences and SoundPool topics. Compose BOM 2026.06.00 and compiler plugin 2.4.0 match the course [Greetings catalog](https://github.com/a159x36/Examples336/blob/main/Greetings/gradle/libs.versions.toml), checked on 21 October 2026. The original wrapper, wrapper properties/JAR, daemon JVM properties, root build script and settings script remain intact.

AndroidX Activity, Compose UI/Foundation/Material 3, Lifecycle, Core KTX and test dependencies are declared in Gradle. No library sources are copied. The module disables optional extra Compose ProGuard mapping entries because AGP's built-in Kotlin would request an unpublished compose-group-mapping:2.2.10 artifact. This affects extra release stack-trace metadata only; see the [Kotlin option documentation](https://kotlinlang.org/api/kotlin-gradle-plugin/compose-compiler-gradle-plugin/org.jetbrains.kotlin.compose.compiler.gradle/-compose-compiler-gradle-plugin-extension/include-compose-mapping-file.html).

## Build and test

Use Android Studio's bundled JDK / the toolchain selected by the existing wrapper.

```powershell
.\gradlew.bat :app:build
# With an emulator or USB-debugging device:
.\gradlew.bat :app:connectedDebugAndroidTest
# Select one device when multiple are connected:
$env:ANDROID_SERIAL = 'emulator-5560'
.\gradlew.bat :app:connectedDebugAndroidTest
```

Unit tests exercise board invariants, 525 simulated turns, every mission type, special counters, boss damage, frozen shields, retaliation timing, enrage and last-move victory. Deterministic planning-player simulations verify a winning path exists for every authored stage without inspecting future refills. Emulator tests cover the English interface, taps/swipes, tools, briefings, ten-stage trail, save migration, persistent boss state, rotation, daily repeatability and win/loss flow.

The standard APK is `app/build/outputs/apk/debug/app-debug.apk`. The current convenience copy is `output/apk/ForestPop-debug.apk`, with screenshots in `output/screenshots/` and verification results/checksum in `output/verification.json`. `tmp/` and `output/` are QA artifacts, not build inputs.

## Submission and assets

Use **Android Studio > File > Export > Zip File...** for the ZIP required by the supplied assignment. The APK is for playing. Review the source and comments and follow your course's rules for acknowledging development assistance.

All characters and interface graphics are original vector geometry in ForestArt.kt or vector launcher XML. WAV effects in res/raw can be regenerated with `python tools/generate_sounds.py`, using only the Python standard library. Fonts are supplied by Android. No external textures or fonts are needed.
