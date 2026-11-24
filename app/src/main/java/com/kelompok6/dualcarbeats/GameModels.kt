package com.kelompok6.dualcarbeats

import android.content.Context
import android.view.View
import org.json.JSONObject

enum class Car { RED, BLUE }
enum class NoteType { NOTE, HOLD, OBSTACLE }
enum class Judgement { PERFECT, GREAT, MISS }

class MapEvent(
    val t: Long,
    val car: Car,
    val type: NoteType,
    val duration: Long = 0
) {
    var lane = 1
    var targetLane = 1
    var state = PENDING
    var holding = false
    var pendingMoveAt = -1L
    var views: List<View> = emptyList()

    companion object {
        const val PENDING = 0
        const val HIT = 1
        const val MISSED = 2
        const val FAILED = 3
    }
}

object MapLoader {
    fun load(context: Context, fileName: String): List<MapEvent> {
        val text = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("events")
        require(arr.length() > 0) { "Map kosong" }

        val list = ArrayList<MapEvent>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val car = when (o.getString("car")) {
                "R" -> Car.RED
                "B" -> Car.BLUE
                else -> error("car harus R atau B (event $i)")
            }
            val type = when (o.optString("type", "note").lowercase()) {
                "hold" -> NoteType.HOLD
                "obstacle" -> NoteType.OBSTACLE
                "note" -> NoteType.NOTE
                else -> error("type tidak dikenal (event $i)")
            }
            val t = o.getLong("t")
            val d = o.optLong("d", 0)
            require(t >= 1800 && (if (type == NoteType.HOLD) d >= 300 else d == 0L)) { "Waktu/durasi tidak valid (event $i)" }
            list.add(MapEvent(t, car, type, d))
        }
        list.sortBy { it.t }

        for (car in Car.values()) {
            list.filter { it.car == car }.zipWithNext().forEach { (a, b) ->
                require(b.t - (a.t + a.duration) > 2 * GameEngine.GREAT_MS) { "Event $car bertumpuk pada ${b.t}" }
            }
        }
        val laneNow = mutableMapOf(Car.RED to 2, Car.BLUE to 4)
        for (ev in list) {
            val cur = laneNow.getValue(ev.car)
            val other = if (ev.car == Car.RED) (if (cur == 2) 1 else 2) else (if (cur == 4) 3 else 4)
            if (ev.type == NoteType.OBSTACLE) {
                ev.lane = cur
                ev.targetLane = other
            } else {
                ev.lane = other
                ev.targetLane = other
            }
            laneNow[ev.car] = ev.targetLane
        }
        return list
    }
}
