package com.kelompok6.dualcarbeats

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// =====================================================
// DATA CLASSES (one for each table in the ERD)
// =====================================================

data class GameLevel(
    val levelId: Int,
    val namaLagu: String,
    val kesulitan: String,
    val fileMusik: String,
    val fileMap: String
)

data class HighScore(
    val scoreId: Int,
    val skorTertinggi: Int,
    val perfectCount: Int,
    val greatCount: Int,
    val missCount: Int,
    val maxCombo: Int,
    val levelId: Int
)

data class Pengaturan(
    val volumeMusik: Int,
    val volumeSfx: Int,
    val getarAktif: Boolean
)

// =====================================================
// DATABASE
// =====================================================

class DatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, "dualcarbeats.db", null, 3) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val sql = context.assets.open("dualcarbeats.sql")
            .bufferedReader().use { it.readText() }

        sql.lines()
            .filter { !it.trim().startsWith("--") }
            .joinToString("\n")
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { db.execSQL(it) }
        createFullComboTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("UPDATE game_level SET nama_lagu='On & On - NCS', kesulitan='Easy', file_musik='music_onandon_2', file_map='map_easy.json' WHERE level_id=1")
            db.execSQL("UPDATE game_level SET nama_lagu='Safe & Sound - NCS', kesulitan='Medium', file_musik='music_safeandsound_1', file_map='map_medium.json' WHERE level_id=2")
            db.execSQL("UPDATE game_level SET file_musik='music_monkeys_3' WHERE level_id=3")
        }
        if (oldVersion < 3) createFullComboTable(db)
    }

    private fun createFullComboTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS full_combo (level_id INTEGER PRIMARY KEY REFERENCES game_level(level_id), chart_hash TEXT NOT NULL)")
    }

    private fun chartHash(levelId: Int): String? {
        val level = getLevel(levelId) ?: return null
        return try {
            val bytes = context.assets.open(level.fileMap).use { it.readBytes() }
            java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        } catch (_: java.io.IOException) { null }
    }

    /** Called only after GameEngine verifies the entire completed run. */
    fun recordFullCombo(levelId: Int) {
        val hash = chartHash(levelId) ?: return
        val values = ContentValues().apply { put("level_id", levelId); put("chart_hash", hash) }
        writableDatabase.insertWithOnConflict("full_combo", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // =================================================
    // GAME_LEVEL
    // =================================================

    fun getAllLevels(): List<GameLevel> {
        val list = mutableListOf<GameLevel>()
        readableDatabase.rawQuery("SELECT * FROM game_level ORDER BY level_id", null).use { c ->
            while (c.moveToNext()) {
                list.add(readLevel(c))
            }
        }
        return list
    }

    fun getLevel(levelId: Int): GameLevel? {
        readableDatabase.rawQuery(
            "SELECT * FROM game_level WHERE level_id = ?",
            arrayOf(levelId.toString())
        ).use { c ->
            if (c.moveToFirst()) return readLevel(c)
        }
        return null
    }

    private fun readLevel(c: android.database.Cursor) = GameLevel(
        c.getInt(c.getColumnIndexOrThrow("level_id")),
        c.getString(c.getColumnIndexOrThrow("nama_lagu")),
        c.getString(c.getColumnIndexOrThrow("kesulitan")),
        c.getString(c.getColumnIndexOrThrow("file_musik")),
        c.getString(c.getColumnIndexOrThrow("file_map"))
    )

    // =================================================
    // HIGH_SCORE
    // =================================================

    fun getHighScore(levelId: Int): HighScore? {
        readableDatabase.rawQuery(
            "SELECT * FROM high_score WHERE game_level_level_id = ?",
            arrayOf(levelId.toString())
        ).use { c ->
            if (c.moveToFirst()) {
                return HighScore(
                    c.getInt(c.getColumnIndexOrThrow("score_id")),
                    c.getInt(c.getColumnIndexOrThrow("skor_tertinggi")),
                    c.getInt(c.getColumnIndexOrThrow("perfect_count")),
                    c.getInt(c.getColumnIndexOrThrow("great_count")),
                    c.getInt(c.getColumnIndexOrThrow("miss_count")),
                    c.getInt(c.getColumnIndexOrThrow("max_combo")),
                    c.getInt(c.getColumnIndexOrThrow("game_level_level_id"))
                )
            }
        }
        return null
    }

    fun saveHighScore(
        levelId: Int,
        score: Int,
        perfect: Int,
        great: Int,
        miss: Int,
        maxCombo: Int
    ): Boolean {
        val old = getHighScore(levelId)
        if (old != null && score <= old.skorTertinggi) return false

        val values = ContentValues().apply {
            put("skor_tertinggi", score)
            put("perfect_count", perfect)
            put("great_count", great)
            put("miss_count", miss)
            put("max_combo", maxCombo)
            put("game_level_level_id", levelId)
        }

        if (old == null) {
            writableDatabase.insert("high_score", null, values)
        } else {
            writableDatabase.update(
                "high_score", values, "game_level_level_id = ?", arrayOf(levelId.toString())
            )
        }
        return true
    }

    fun isFullCombo(levelId: Int): Boolean {
        val hash = chartHash(levelId) ?: return false
        readableDatabase.rawQuery("SELECT chart_hash FROM full_combo WHERE level_id = ?", arrayOf(levelId.toString())).use {
            return it.moveToFirst() && it.getString(0) == hash
        }
    }

    fun resetHighScores() {
        writableDatabase.delete("high_score", null, null)
        writableDatabase.delete("full_combo", null, null)
    }

    // =================================================
    // PENGATURAN (settings)
    // =================================================

    fun getPengaturan(): Pengaturan {
        readableDatabase.rawQuery("SELECT * FROM pengaturan WHERE setting_id = 1", null).use { c ->
            if (c.moveToFirst()) {
                return Pengaturan(
                    c.getInt(c.getColumnIndexOrThrow("volume_musik")).coerceIn(0, 100),
                    c.getInt(c.getColumnIndexOrThrow("volume_sfx")).coerceIn(0, 100),
                    c.getInt(c.getColumnIndexOrThrow("getar_aktif")) == 1
                )
            }
        }
        return Pengaturan(60, 75, true)
    }

    fun savePengaturan(p: Pengaturan) {
        val values = ContentValues().apply {
            put("setting_id", 1)
            put("volume_musik", p.volumeMusik.coerceIn(0, 100))
            put("volume_sfx", p.volumeSfx.coerceIn(0, 100))
            put("getar_aktif", if (p.getarAktif) 1 else 0)
        }
        writableDatabase.insertWithOnConflict("pengaturan", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}