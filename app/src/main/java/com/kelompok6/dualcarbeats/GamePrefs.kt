package com.kelompok6.dualcarbeats

import android.content.Context

object GamePrefs {
    private const val FILE = "dualcarbeats_prefs"

    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getSpeed(c: Context): Int = prefs(c).getInt("note_speed", 5).coerceIn(1, 10)
    fun setSpeed(c: Context, v: Int) = prefs(c).edit().putInt("note_speed", v.coerceIn(1, 10)).apply()

    fun getOffset(c: Context): Int = prefs(c).getInt("audio_offset", 0).coerceIn(-100, 100)
    fun setOffset(c: Context, v: Int) = prefs(c).edit().putInt("audio_offset", v.coerceIn(-100, 100)).apply()
}
