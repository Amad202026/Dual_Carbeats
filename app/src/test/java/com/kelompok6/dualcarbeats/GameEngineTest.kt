package com.kelompok6.dualcarbeats

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    private class Sink : GameListener {
        val moves = mutableListOf<Pair<Car, Int>>()
        val results = mutableListOf<Boolean>()
        var fevers = 0; var feverEnds = 0; var invalid = 0
        override fun onJudgement(judgement: Judgement, event: MapEvent) {}
        override fun onCarMove(car: Car, toLane: Int) { moves.add(car to toLane) }
        override fun onCrash(event: MapEvent) {}
        override fun onStatsChanged(score: Int, combo: Int) {}
        override fun onHpChanged(hp: Int) {}
        override fun onFeverStart() { fevers++ }
        override fun onFeverEnd() { feverEnds++ }
        override fun onInvalidTap(car: Car) { invalid++ }
        override fun onGameEnd(win: Boolean) { results.add(win) }
    }
    private fun note(t: Long, car: Car = Car.RED, type: NoteType = NoteType.NOTE, d: Long = 0) =
        MapEvent(t, car, type, d).apply { targetLane = if (car == Car.RED) 1 else 3 }

    @Test fun independentSimultaneousCars() {
        val sink = Sink(); val e = GameEngine(listOf(note(1000), note(1000, Car.BLUE)), sink)
        e.onTouchDown(Car.RED, 1000); e.onTouchDown(Car.BLUE, 1000)
        assertEquals(2, e.perfect); assertEquals(600, e.score)
        assertEquals(listOf(Car.RED to 1, Car.BLUE to 3), sink.moves)
    }
    @Test fun invalidTapDoesNotMoveOrScoreOrCountMiss() {
        val sink = Sink(); val e = GameEngine(listOf(note(1000)), sink)
        e.onTouchDown(Car.RED, 1000); e.onTouchDown(Car.RED, 1010)
        assertEquals(0, e.combo); assertEquals(1, e.maxCombo)
        assertEquals(300, e.score); assertEquals(0, e.miss); assertEquals(1, sink.moves.size)
    }
    @Test fun timingBoundaries() {
        for ((delta, perfect, great, miss) in listOf(listOf(-180,0,1,0), listOf(-75,1,0,0), listOf(75,1,0,0), listOf(180,0,1,0), listOf(181,0,0,1))) {
            val e = GameEngine(listOf(note(1000)), Sink()); e.onTouchDown(Car.RED, 1000L + delta)
            assertEquals(perfect, e.perfect); assertEquals(great, e.great); assertEquals(miss, e.miss)
        }
    }
    @Test fun missedNoteResyncsWithoutHpDamage() {
        val sink = Sink(); val e = GameEngine(listOf(note(1000)), sink); e.update(1181)
        assertEquals(1, e.miss); assertEquals(3, e.hp); assertEquals(1, sink.moves.size)
    }
    @Test fun successfulHoldCountsOnceAtEnd() {
        val e = GameEngine(listOf(note(1000, type=NoteType.HOLD, d=1000)), Sink())
        e.onTouchDown(Car.RED, 1080); assertEquals(0, e.score)
        e.update(2000); e.onTouchUp(Car.RED, 2010); e.update(3000)
        assertEquals(1, e.great); assertEquals(150, e.score); assertEquals(0, e.miss)
    }
    @Test fun earlyHoldReleaseOnlyMissAndResyncAtEnd() {
        val sink = Sink(); val e = GameEngine(listOf(note(1000, type=NoteType.HOLD, d=1000)), sink)
        e.onTouchDown(Car.RED, 1000); e.onTouchUp(Car.RED, 1500)
        assertEquals(0, e.perfect); assertEquals(0, e.score); assertEquals(1, e.miss)
        assertEquals(1, sink.moves.size); e.update(2000); assertEquals(2, sink.moves.size)
    }
    @Test fun holdReleaseToleranceBoundary() {
        for (release in listOf(1899L, 1900L)) {
            val e = GameEngine(listOf(note(1000, type=NoteType.HOLD, d=1000)), Sink())
            e.onTouchDown(Car.RED, 1000); e.onTouchUp(Car.RED, release)
            assertEquals(if (release == 1900L) 1 else 0, e.perfect)
            assertEquals(if (release == 1899L) 1 else 0, e.miss)
        }
    }
    @Test fun missedHoldMovesAtTail() {
        val sink = Sink(); val e = GameEngine(listOf(note(1000, type=NoteType.HOLD, d=1000)), sink)
        e.update(1200); assertEquals(0, sink.moves.size); e.update(2000)
        assertEquals(1, sink.moves.size); assertEquals(1, e.miss)
    }
    @Test fun cancelFailsHoldAndOtherCarRemainsPlayable() {
        val e = GameEngine(listOf(note(1000, type=NoteType.HOLD, d=1000),note(1500,Car.BLUE)), Sink())
        e.onTouchDown(Car.RED, 1000); e.cancelTouches(1300); e.onTouchDown(Car.BLUE,1500)
        assertEquals(1,e.miss); assertEquals(1,e.perfect)
    }
    @Test fun coneCollisionAfterFrontWindowExpires() {
        val e = GameEngine(listOf(note(1000,type=NoteType.OBSTACLE),note(2000,type=NoteType.OBSTACLE)), Sink())
        e.onTouchDown(Car.RED, 950); e.onTouchDown(Car.RED, 2181)
        assertEquals(1,e.perfect); assertEquals(1,e.miss); assertEquals(2,e.hp)
    }
    @Test fun threeCrashesEndExactlyOnce() {
        val sink = Sink(); val e = GameEngine((1..4).map { note(it*1000L,type=NoteType.OBSTACLE) },sink)
        e.update(5000); e.onSongCompleted(6000)
        assertEquals(0,e.hp); assertEquals(3,e.miss); assertEquals(listOf(false),sink.results)
    }
    @Test fun onlyMusicCompletionWins() {
        val sink = Sink(); val e = GameEngine(listOf(note(1000)),sink)
        e.onTouchDown(Car.RED,1000); e.update(100000); assertTrue(sink.results.isEmpty())
        e.onSongCompleted(110000); e.onSongCompleted(110001); assertEquals(listOf(true),sink.results)
    }
    @Test fun feverMultiplesBonusAndExpiry() {
        val sink=Sink(); val e=GameEngine((1..201).map { note(it*300L) },sink)
        for(i in 1..100) e.onTouchDown(Car.RED,i*300L)
        assertEquals(1,sink.fevers); assertEquals(30000,e.score)
        e.onTouchDown(Car.RED,30300); assertEquals(30600,e.score)
        for(i in 102..200) e.onTouchDown(Car.RED,i*300L)
        assertEquals(2,sink.fevers); assertEquals(1,sink.feverEnds)
        val old=e.score; e.onTouchDown(Car.RED,60300); assertEquals(old+600,e.score)
        e.update(70000); assertEquals(2,sink.feverEnds)
    }
    @Test fun overdueEventsResolveAcrossCarsInTimeOrder() {
        val sink=Sink(); val e=GameEngine(listOf(note(1000,Car.BLUE),note(2000,Car.RED)),sink)
        e.update(3000); assertEquals(listOf(Car.BLUE to 3,Car.RED to 1),sink.moves)
    }

    @Test fun coneAndNoteShareFrontWindow() {
        for (type in listOf(NoteType.NOTE, NoteType.OBSTACLE, NoteType.HOLD)) {
            for (delta in listOf(-180L, -75L, 0L, 75L, 180L)) {
                val event = note(1000, type=type, d=if (type == NoteType.HOLD) 1000L else 0L)
                val e = GameEngine(listOf(event), Sink())
                e.onTouchDown(Car.RED, 1000 + delta)
                if (type == NoteType.HOLD) e.onTouchUp(Car.RED,2000)
                assertEquals(1,e.perfect + e.great)
                assertEquals(0,e.miss); assertEquals(3,e.hp)
            }
        }
    }
    @Test fun fullComboRequiresWholeSongAndAllEvents() {
        val e=GameEngine(listOf(note(1000),note(2000,Car.BLUE)),Sink())
        e.onTouchDown(Car.RED,1000); e.onTouchDown(Car.BLUE,2000)
        assertFalse(e.fullCombo)
        e.onSongCompleted(5000); assertTrue(e.fullCombo)
    }
    @Test fun invalidTapInOutroDisqualifiesFullCombo() {
        val e=GameEngine(listOf(note(1000)),Sink())
        e.onTouchDown(Car.RED,1000); e.onTouchDown(Car.BLUE,3000)
        e.onSongCompleted(5000)
        assertEquals(0,e.miss); assertEquals(1,e.invalidTaps); assertFalse(e.fullCombo)
    }
    @Test fun missedHoldOrEmptyChartCannotEarnFullCombo() {
        val e=GameEngine(listOf(note(1000,type=NoteType.HOLD,d=1000)),Sink())
        e.onTouchDown(Car.RED,1000); e.onTouchUp(Car.RED,1500); e.onSongCompleted(5000)
        assertFalse(e.fullCombo)
        val empty=GameEngine(emptyList(),Sink()); empty.onSongCompleted(1000); assertFalse(empty.fullCombo)
    }
}
