> **Current UI revision: source only, not built.** The verification results below describe the earlier gameplay revision. See [UI_UPDATE.md](UI_UPDATE.md) and [UI_CHECKS.txt](UI_CHECKS.txt) for the current changes and limited checks.

# Dual Carbeats — implementation report

## Deliverables and provenance
This is a modified copy of the existing Android Studio project, not a replacement app. Source/resources were extracted from `C:/Users/ASUS/AndroidStudioProjects/DualCarbeats/app/src/main.rar`. Gradle wrapper, version catalog, build configuration and existing tests were copied from the enclosing DualCarbeats project because the archive contains only app/src/main. The original project and archive were not modified. Open this folder in Android Studio.

## Architecture inspected
Kotlin; Android ComponentActivity screens with XML layouts; GameActivity renders ImageViews inside four FrameLayout lanes using Choreographer. GameEngine handles judgement/stats. MapLoader reads JSON using org.json and assigns deterministic alternating lanes. SQLiteOpenHelper stores levels, settings and highscores. MediaPlayer supplies BGM/song playback, SoundPool supplies short effects. Existing Compose dependencies/theme files remain, but the gameplay screen is XML-based. Original artwork, fonts and frame animations remain intact.

## Changes
- Independent red/blue pointer ownership; a second finger on the same side cannot steal/release an existing hold. Moving across the middle retains the original side until release.
- Music playback position drives input judgement and object movement, rather than a drifting wall clock.
- Perfect <=60 ms, Great <=130 ms; cones accept only the 130 ms before collision, with Perfect in the final 60 ms. Cone failure costs one of three HP; ordinary misses cost no HP.
- Holds score exactly once at completion. Early release/cancel produces one Miss without keeping a premature Perfect or score. Missed/failed holds resynchronize at the tail. Release tolerance is 100 ms.
- Invalid taps leave the car in place, score nothing, and break combo. They show MISS feedback but do not inflate missed-note counts.
- Full statistics and max combo; Fever at every 100-combo multiple, 2x points for 10 seconds. Subsequent activation resets the expiry to 10 seconds from that hit.
- WIN is triggered by the actual music completion callback; GAME OVER is immediate at zero HP. Results explicitly label WIN/GAME OVER and retain all requested statistics.
- Cars reduced from 68x140 dp to 48x99 dp. Existing hit/crash/fever sprites retained, hit animation restarted on repeated hits, judgement pulses added, hold starts receive immediate hit feedback.
- Correct music filenames and difficulty ordering; database v2 migration updates level rows without dropping saved settings or high scores. Older scores are retained but may not be comparable to the new charts.
- Static Easy/Medium/Hard maps contain 253/352/472 events. Explicit authored phrases/arrangements use measured beat timestamps from supplied MP3 files. No map generator is included in the app.
- Audio manager maps actual supplied filenames, handles load completion, music/SFX volume independently, focus request/denial/loss, headphone unplug, pause/background/countdown cleanup and resource release. Focus interruption pauses gameplay; resume is explicit. Menu BGM and button press SFX use existing assets.
- Chart validation rejects unknown cars/types, invalid durations, same-car overlaps, and events extending past the selected music.

## Assets and assumptions
All 10 files in res/raw are byte-for-byte identical to the archive. No music is missing: On & On (~208 s), Safe & Sound (~204 s), Monkeys Spinning Monkeys (~125 s), plus menu BGM. No dedicated fever/countdown WAV was supplied. Fever reuses sfx_poin at 1.25x pitch; countdown reuses sfx_button. Hit uses sfx_poin, crash uses sfx_damage, game-over uses sfx_loss; existing miss/win/button files are used directly.

The JSON charts are initial authored arrangements, not a claim of human-verified transcription. Musical accent quality and physical-device latency still require listening/playtesting. There are deliberate rests/outros after the final note, particularly Medium (~171.5 s through music end ~204.3 s). The full recording always plays to completion on a surviving run. Team members can refine timestamps/events using MAP_AUTHORING_ID.md.

Pause/cancel is treated as releasing a hold, so an unfinished hold can become Miss. There is no automatic hold reattachment or countdown on resume. HP is not regenerated. Positive calibration offset advances chart timing relative to the music. Menu tracks restart when changing menu screens. Dedicated physical-device multi-touch, Bluetooth latency, incoming-call and headphone-route acceptance testing remains recommended.

Audio focus implementation reference: https://developer.android.com/media/optimize/audio-focus

## Verification
- assembleDebug: successful; debug APK included separately.
- testDebugUnitTest: 14 gameplay regression tests + 1 existing example test passed; zero failures.
- lintDebug: successful, zero errors; 220 warnings on the checked build, largely existing asset/layout/dependency warnings. This is not a warning-free cleanup.
- connectedDebugAndroidTest on Medium_Phone / Android 17 emulator: 2 tests passed. MapIntegrationTest loads all three actual charts, reads packaged MP3 durations and simulates all inputs to full combo/WIN without HP loss. Existing application-context test also passes.
- JSON same-car spacing/hold overlap checks and byte-for-byte source audio preservation checks passed.
- Emulator ran without audio output, so this does not verify audible mixing, perceived sync or latency.

## Exact source changes relative to main.rar
- Modified `app/src/main/AndroidManifest.xml`
- Modified `app/src/main/assets/dualcarbeats.sql`
- Modified `app/src/main/assets/map_easy.json`
- Modified `app/src/main/java/com/kelompok6/dualcarbeats/BaseActivity.kt`
- Modified `app/src/main/java/com/kelompok6/dualcarbeats/DatabaseHelper.kt`
- Modified `app/src/main/java/com/kelompok6/dualcarbeats/GameActivity.kt`
- Modified `app/src/main/java/com/kelompok6/dualcarbeats/GameAudio.kt`
- Modified `app/src/main/java/com/kelompok6/dualcarbeats/GameEngine.kt`
- Modified `app/src/main/java/com/kelompok6/dualcarbeats/GameModels.kt`
- Modified `app/src/main/java/com/kelompok6/dualcarbeats/ResultActivity.kt`
- Modified `app/src/main/res/layout/activity_game.xml`
- Modified `app/src/main/res/layout/activity_select_map.xml`
- Added `app/src/main/assets/map_hard.json`
- Added `app/src/main/assets/map_medium.json`
- Added `app/src/test/java/com/kelompok6/dualcarbeats/GameEngineTest.kt`
- Added `app/src/androidTest/java/com/kelompok6/dualcarbeats/MapIntegrationTest.kt`
- Added `MAP_AUTHORING_ID.md`
- Added `CHANGE_REPORT.md`

Gradle/build configuration is unchanged from the enclosing original project. The ZIP excludes generated build/cache directories and machine-specific local.properties. The APK is debug-signed, for testing; produce a properly signed release through your normal Android Studio workflow when ready.
