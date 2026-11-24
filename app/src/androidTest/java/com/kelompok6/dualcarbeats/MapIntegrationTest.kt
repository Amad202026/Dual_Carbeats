package com.kelompok6.dualcarbeats

import android.media.MediaMetadataRetriever
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapIntegrationTest {
    private class Sink : GameListener {
        var result: Boolean? = null
        override fun onJudgement(judgement: Judgement, event: MapEvent) {}
        override fun onCarMove(car: Car, toLane: Int) {
            assertTrue(if (car == Car.RED) toLane in 1..2 else toLane in 3..4)
        }
        override fun onCrash(event: MapEvent) {}
        override fun onStatsChanged(score: Int, combo: Int) {}
        override fun onHpChanged(hp: Int) {}
        override fun onFeverStart() {}
        override fun onFeverEnd() {}
        override fun onInvalidTap(car: Car) { fail("Authored perfect route must not contain invalid taps") }
        override fun onGameEnd(win: Boolean) { result = win }
    }
    @Test fun everyPackagedChartIsPlayableAndFitsItsUnmodifiedRecording() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = DatabaseHelper(context)
        val levels = db.getAllLevels()
        assertEquals(3, levels.size)
        for (level in levels) {
            val events = MapLoader.load(context, level.fileMap)
            val id = context.resources.getIdentifier(level.fileMusik, "raw", context.packageName)
            assertTrue(id != 0)
            val retriever = MediaMetadataRetriever()
            val duration = context.resources.openRawResourceFd(id).use {
                retriever.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
            }
            retriever.release()
            assertTrue(events.all { it.t + it.duration + GameEngine.GREAT_MS < duration })
            val sink = Sink(); val engine = GameEngine(events,sink)
            val inputs = events.flatMap { ev ->
                if (ev.type == NoteType.HOLD) listOf(Triple(ev.t, ev.car, true), Triple(ev.t+ev.duration,ev.car,false))
                else listOf(Triple(ev.t,ev.car,true))
            }.sortedBy { it.first }
            for ((time,car,down) in inputs) {
                if (down) engine.onTouchDown(car,time) else engine.onTouchUp(car,time)
            }
            engine.update(duration-1)
            assertNull(sink.result)
            engine.onSongCompleted(duration)
            assertEquals(true,sink.result)
            assertTrue(engine.fullCombo)
            assertEquals(events.size,engine.perfect)
            assertEquals(events.size,engine.maxCombo)
            assertEquals(0,engine.miss)
            assertEquals(3,engine.hp)
        }
        db.close()
    }
}
