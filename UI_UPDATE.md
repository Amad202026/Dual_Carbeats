# Latest revision — November 11: original notes, fullscreen road, constant motion

Changed directly in this project folder. No build or ZIP.

- Restored original red_not/blue_not resources, including hold endpoints; removed the glossy note rendering wrapper.
- Road/lane area fills the window. A separate transparent HUD receives safe margins/insets, keeping buttons and text away from cutouts and borders. The original road image and its scrolling markings remain.
- Removed the 6 dp / 180 ms visual slowdown. Notes/cones travel at constant song-clock velocity through the hit point. A successfully held head still stays at its target while its tail is consumed, as intended.
- Perfect target gap reduced from 10 dp to 4 dp relative to the object's layout bounds. Judgement timing windows are unchanged; late Great inputs now occur while the object continues moving rather than pausing near the bumper.
- Checks passed as listed in UI_CHECKS.txt. This revision has not been compiled or run on a phone.

Changed files: BaseActivity.kt, GameActivity.kt, NeonDrawables.kt, res/layout/activity_game.xml (plus these update/check notes).

---
Previous revision notes follow for history.

# Latest direct-folder revision: menu BGM, original road, closer target, settings

Edited this project folder directly. No build, ZIP, replacement image or audio asset was created.

- Menu BGM now has one application-context player shared across menu screens. Navigation keeps the playback position. Entering gameplay or leaving the app pauses it; returning resumes it. Settings updates change volume without reloading the music.
- Gameplay now draws the supplied `res/drawable/road.png`, with only its existing dashed cyan markings moving downward. The image file itself is unchanged. The previous synthesized road, extra target lines and target boxes are removed.
- The target is closer to the bumper. During late timing tolerance, objects advance only another 6 dp, staying ahead of the car. Hold tail endpoints follow the same timing-to-position calculation.
- Music and SFX sliders preview immediately; releasing the SFX slider plays a sample. Save persists changes; Back restores saved volumes. The vibration switch previews when enabled and the saved flag controls gameplay vibration. Note speed stays 1–10, audio offset -100–100 ms, both committed with Save. Reset now explicitly mentions that FC badges are also cleared. Missing settings rows can be saved/recreated.
- Source/resource checks, original-asset comparisons, geometry checks and SQLite settings round trips passed. **Not compiled or tested on a running phone in this revision.** See UI_CHECKS.txt for the exact scope.

Changed Kotlin files:
- `app/src/main/java/com/kelompok6/dualcarbeats/BaseActivity.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/DatabaseHelper.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/GameActivity.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/GameAudio.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/GamePrefs.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/NeonDrawables.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/OtherActivity.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/SettingsActivity.kt`

---

The notes below describe the previous source revision and its old ZIP delivery; they are historical.

# Dual Carbeats UI / front-target update

Source update only. **No app build, APK generation, Gradle tests or emulator run was performed for this revision**, as requested. Earlier build results in CHANGE_REPORT.md refer to the previous revision.

## Apply
If you already use the previous fixed project, extract `DualCarbeats-ui-update.zip` and copy its `app` directory into your project, merging folders and replacing the listed files. Back up your current source first. Include the new `NeonDrawables.kt` file. No new dependencies or Gradle changes are needed.

Alternatively, extract `DualCarbeats-ui-source.zip` and open its `DualCarbeats` folder in Android Studio. Build it yourself. That archive contains the complete updated source, without old APKs, build/cache directories or machine-specific local.properties.

The working source copy is in this outputs/DualCarbeats folder. Your original AndroidStudioProjects folder was not overwritten.

## What changed
- Preserve each screen's XML padding and add stable system-bar/cutout insets, with minimum 16 dp side, 12 dp top and 28 dp bottom breathing room. This fixes the shared code that previously erased the screen margins.
- Cars sit at 80% of the playable road height. MISS/PERFECT/GREAT are centered at roughly 43% of the road height. Combo text and label disappear at zero; 3-2-1 countdown remains visible.
- Notes, holds and cones share a target band ahead of the front bumper. Perfect is +/-75 ms; Great/dodge is +/-180 ms around the musical timestamp. Cones collide after that window expires. The displayed target moves with viewport/speed calculations so the object is still fully in front at the latest accepted input; there is no collection window beside the car body.
- Hit/crash effects follow the actual car center during the lane-change animation. Car halos follow the same motion.
- Scrolling cyan road dividers use the song clock, so scrolling freezes on pause/background and resumes in sync. A darker purple/blue road, soft edge shading, colored target bands, note halos/rims/highlights and subtle red/blue underglow add depth. Original car PNGs, note vectors, FX sprites, music and sounds are unchanged.
- Level selection content moves upward and scrolls if needed. Result titles can wrap; result statistics scroll on shorter screens. Bottom buttons stay inside safe margins.
- FC requires reaching the end of the music, hitting every event (including completed holds), no Miss and no invalid tap. A new database-v3 table saves verified FC separately from high score, tied to the current JSON content. Existing scores remain, but old/unverified FC badges start hidden; earn FC once on the updated version. Later non-FC runs do not erase an earned badge. Editing the map invalidates its old badge. Badges refresh when returning to level selection.

## Checks performed
See UI_CHECKS.txt. XML/resource parsing, chart timing-spacing checks, numeric geometry checks at six phone sizes and ten note speeds, and execution of the actual FC-table SQL/migration checks passed. Original assets and maps were compared byte-for-byte with the previous revision.

These checks do not compile Kotlin or render the Android screens. JUnit timing/FC tests and the map integration test were updated for your next test run; they were not run in this revision. Phone playtesting remains necessary, especially cutouts/gesture navigation, multitouch holds and visual glow intensity.

## Files changed in this revision
- `app/src/androidTest/java/com/kelompok6/dualcarbeats/MapIntegrationTest.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/BaseActivity.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/DatabaseHelper.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/GameActivity.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/GameEngine.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/NeonDrawables.kt`
- `app/src/main/java/com/kelompok6/dualcarbeats/SelectMapActivity.kt`
- `app/src/main/res/layout/activity_game.xml`
- `app/src/main/res/layout/activity_result.xml`
- `app/src/main/res/layout/activity_select_map.xml`
- `app/src/test/java/com/kelompok6/dualcarbeats/GameEngineTest.kt`
- `MAP_AUTHORING_ID.md` (updated timing and FC guidance)
