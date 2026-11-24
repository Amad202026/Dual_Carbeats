package com.kelompok6.dualcarbeats

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView

class ResultActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val db = DatabaseHelper(this)

        val levelId = intent.getIntExtra(Extras.LEVEL_ID, 1)
        val win = intent.getBooleanExtra(Extras.WIN, false)
        val score = intent.getIntExtra(Extras.SCORE, 0)
        val perfect = intent.getIntExtra(Extras.PERFECT, 0)
        val great = intent.getIntExtra(Extras.GREAT, 0)
        val miss = intent.getIntExtra(Extras.MISS, 0)
        val maxCombo = intent.getIntExtra(Extras.MAX_COMBO, 0)

        if (win) db.saveHighScore(levelId, score, perfect, great, miss, maxCombo)

        val level = db.getLevel(levelId)
        findViewById<TextView>(R.id.tvSongTitle).text =
            (if (win) "WIN\n" else "GAME OVER\n") + (if (level != null) "${level.namaLagu} (${level.kesulitan})" else "")
        findViewById<TextView>(R.id.tvFinalScore).text = score.toString()
        findViewById<TextView>(R.id.tvPerfect).text = perfect.toString()
        findViewById<TextView>(R.id.tvGreat).text = great.toString()
        findViewById<TextView>(R.id.tvMiss).text = miss.toString()
        findViewById<TextView>(R.id.tvMaxCombo).text = maxCombo.toString()
        findViewById<TextView>(R.id.tvHighScore).text =
            (db.getHighScore(levelId)?.skorTertinggi ?: 0).toString()
        findViewById<TextView>(R.id.tvRank).text = rankOf(win, perfect, great, miss)

        findViewById<View>(R.id.btnRetry).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java).putExtra(Extras.LEVEL_ID, levelId))
            finish()
        }
        findViewById<View>(R.id.btnMenu).setOnClickListener {
            startActivity(
                Intent(this, SelectMapActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        findViewById<View>(R.id.btnHome).setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        playResultIntro(this)
    }

    private fun rankOf(win: Boolean, perfect: Int, great: Int, miss: Int): String {
        if (!win) return "C"
        val total = perfect + great + miss
        if (total == 0) return "C"
        val accuracy = (perfect + great * 0.5) / total
        return when {
            accuracy >= 0.95 -> "S"
            accuracy >= 0.85 -> "A"
            accuracy >= 0.70 -> "B"
            else -> "C"
        }
    }
}
