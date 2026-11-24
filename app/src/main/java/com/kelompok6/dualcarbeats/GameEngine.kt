package com.kelompok6.dualcarbeats

import kotlin.math.abs

interface GameListener {
    fun onHoldStart(event: MapEvent) {}
    fun onJudgement(judgement: Judgement, event: MapEvent)
    fun onCarMove(car: Car, toLane: Int)
    fun onCrash(event: MapEvent)
    fun onStatsChanged(score: Int, combo: Int)
    fun onHpChanged(hp: Int)
    fun onFeverStart()
    fun onFeverEnd()
    fun onInvalidTap(car: Car)
    fun onGameEnd(win: Boolean)
}

/** Deterministic song-clock simulation. One final judgement per map event. */
class GameEngine(private val events: List<MapEvent>, private val listener: GameListener) {
    companion object {
        const val PERFECT_MS = 75L
        const val GREAT_MS = 180L
        const val HOLD_TOLERANCE_MS = 100L
        const val FEVER_MS = 10_000L
        const val PERFECT_POINTS = 300
        const val GREAT_POINTS = 150
        const val MAX_HP = 3
    }
    var score = 0; private set
    var combo = 0; private set
    var maxCombo = 0; private set
    var perfect = 0; private set
    var great = 0; private set
    var miss = 0; private set
    var hp = MAX_HP; private set
    private val holds = mutableMapOf<Car, MapEvent>()
    private val holdGrades = mutableMapOf<Car, Judgement>()
    private var feverUntil = -1L
    private var ended = false
    private var won = false
    var invalidTaps = 0; private set
    val fullCombo: Boolean
        get() = ended && won && events.isNotEmpty() && miss == 0 && invalidTaps == 0 &&
            perfect + great == events.size && maxCombo == events.size
    private var now = 0L

    fun onTouchDown(car: Car, time: Long) {
        update(time)
        if (ended) return
        if (holds.containsKey(car)) { invalid(car); return }
        val ev = events.firstOrNull { it.car == car && it.state == MapEvent.PENDING }
        val delta = if (ev != null) time - ev.t else Long.MAX_VALUE
        if (ev == null || abs(delta) > GREAT_MS) {
            invalid(car); return
        }
        ev.state = MapEvent.HIT
        listener.onCarMove(car, ev.targetLane)
        val grade = if (abs(delta) <= PERFECT_MS) Judgement.PERFECT else Judgement.GREAT
        if (ev.type == NoteType.HOLD) {
            ev.holding = true
            holds[car] = ev
            holdGrades[car] = grade
            listener.onHoldStart(ev)
        } else registerHit(grade, ev)
    }
    private fun invalid(car: Car) {
        invalidTaps++
        combo = 0
        listener.onInvalidTap(car)
        listener.onStatsChanged(score, combo)
    }
    fun onTouchUp(car: Car, time: Long) {
        update(time)
        if (ended) return
        val ev = holds.remove(car) ?: return
        ev.holding = false
        val grade = holdGrades.remove(car) ?: Judgement.GREAT
        if (time >= ev.t + ev.duration - HOLD_TOLERANCE_MS) registerHit(grade, ev)
        else {
            ev.state = MapEvent.FAILED
            ev.pendingMoveAt = ev.t + ev.duration
            registerMiss(ev)
        }
    }
    /** Cancellation is a real release; the game never retains a ghost finger. */
    fun cancelTouches(time: Long) { holds.keys.toList().forEach { onTouchUp(it, time) } }

    fun update(time: Long) {
        if (ended) return
        now = time
        if (feverUntil >= 0 && time >= feverUntil) {
            feverUntil = -1
            listener.onFeverEnd()
        }
        // Resolve deadlines in musical order, independent of car or frame rate.
        val due = events.filter {
            (it.state == MapEvent.PENDING && time > it.t + GREAT_MS) ||
                (it.holding && time >= it.t + it.duration)
        }.sortedBy { if (it.holding) it.t + it.duration else it.t + GREAT_MS }
        for (ev in due) {
            if (ev.holding) {
                ev.holding = false
                holds.remove(ev.car)
                registerHit(holdGrades.remove(ev.car) ?: Judgement.GREAT, ev)
            } else {
                ev.state = MapEvent.MISSED
                if (ev.type == NoteType.HOLD) ev.pendingMoveAt = ev.t + ev.duration
                else listener.onCarMove(ev.car, ev.targetLane)
                if (ev.type == NoteType.OBSTACLE) {
                    hp--
                    listener.onHpChanged(hp)
                    listener.onCrash(ev)
                }
                registerMiss(ev)
                if (hp == 0) { finish(false); return }
            }
        }
        events.filter { it.pendingMoveAt >= 0 && time >= it.pendingMoveAt }.forEach {
            listener.onCarMove(it.car, it.targetLane)
            it.pendingMoveAt = -1
        }
    }
    /** Only the music player's completion callback can award WIN. */
    fun onSongCompleted(time: Long) {
        update(time)
        if (!ended) finish(true)
    }
    private fun registerHit(j: Judgement, ev: MapEvent) {
        if (j == Judgement.PERFECT) perfect++ else great++
        val points = if (j == Judgement.PERFECT) PERFECT_POINTS else GREAT_POINTS
        score += if (now < feverUntil) points * 2 else points
        combo++
        maxCombo = maxOf(maxCombo, combo)
        listener.onJudgement(j, ev)
        listener.onStatsChanged(score, combo)
        if (combo % 50 == 0) {
            feverUntil = now + FEVER_MS
            listener.onFeverStart()
        }
    }
    private fun registerMiss(ev: MapEvent) {
        miss++; combo = 0
        listener.onJudgement(Judgement.MISS, ev)
        listener.onStatsChanged(score, combo)
    }
    private fun finish(win: Boolean) {
        if (ended) return
        ended = true
        won = win
        listener.onGameEnd(win)
    }
}
