package com.kelompok6.dualcarbeats

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView

class SelectMapActivity : BaseActivity() {

    private var selectedLevel = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_map)

        val cards = listOf(R.id.cardLevel1, R.id.cardLevel2, R.id.cardLevel3)
            .map { findViewById<View>(it) }
        for (i in 0..2) {
            val level = i + 1
            cards[i].setOnClickListener {
                selectedLevel = level
                cards.forEach { it.isSelected = false }
                cards[i].isSelected = true
            }
        }
        cards[0].isSelected = true

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnPlay).setOnClickListener {
            startActivity(
                Intent(this, GameActivity::class.java).putExtra(Extras.LEVEL_ID, selectedLevel)
            )
        }
    }
    override fun onResume() {
        super.onResume()
        val db = DatabaseHelper(this)
        val scores = listOf(R.id.tvHighscore1, R.id.tvHighscore2, R.id.tvHighscore3)
        val badges = listOf(R.id.ivFc1, R.id.ivFc2, R.id.ivFc3)
        for (i in 0..2) {
            findViewById<TextView>(scores[i]).text = (db.getHighScore(i + 1)?.skorTertinggi ?: 0).toString()
            findViewById<View>(badges[i]).visibility = if (db.isFullCombo(i + 1)) View.VISIBLE else View.GONE
        }
        db.close()
    }

}
