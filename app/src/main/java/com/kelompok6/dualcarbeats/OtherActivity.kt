package com.kelompok6.dualcarbeats

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class OtherActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other)

        val seekSpeed = findViewById<SeekBar>(R.id.seekSpeed)
        val seekOffset = findViewById<SeekBar>(R.id.seekOffset)
        val tvSpeed = findViewById<TextView>(R.id.tvSpeedValue)
        val tvOffset = findViewById<TextView>(R.id.tvOffsetValue)

        seekSpeed.progress = GamePrefs.getSpeed(this) - 1
        tvSpeed.text = (seekSpeed.progress + 1).toString()
        seekSpeed.onChange { tvSpeed.text = (it + 1).toString() }

        seekOffset.progress = GamePrefs.getOffset(this) + 100
        tvOffset.text = "${seekOffset.progress - 100} ms"
        seekOffset.onChange { tvOffset.text = "${it - 100} ms" }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.rowCredit).setOnClickListener {
            startActivity(Intent(this, CreditActivity::class.java))
        }
        findViewById<View>(R.id.rowReset).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Highscore")
                .setMessage("Semua highscore dan badge FC akan dihapus. Lanjutkan?")
                .setPositiveButton("Reset") { _, _ ->
                    val db = DatabaseHelper(this)
                    try { db.resetHighScores() } finally { db.close() }
                    Toast.makeText(this, "Highscore direset", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
        findViewById<View>(R.id.btnSave).setOnClickListener {
            GamePrefs.setSpeed(this, seekSpeed.progress + 1)
            GamePrefs.setOffset(this, seekOffset.progress - 100)
            Toast.makeText(this, "Tersimpan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

private fun SeekBar.onChange(action: (Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = action(progress)
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    })
}
