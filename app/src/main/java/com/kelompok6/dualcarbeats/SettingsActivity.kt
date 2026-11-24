package com.kelompok6.dualcarbeats

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Toast

class SettingsActivity : BaseActivity() {
    private lateinit var db: DatabaseHelper
    private lateinit var seekMusic: SeekBar
    private lateinit var seekSfx: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        db = DatabaseHelper(this)
        seekMusic = findViewById(R.id.seekMusic)
        seekSfx = findViewById(R.id.seekSfx)
        val switchVibration = findViewById<CompoundButton>(R.id.switchVibration)
        val saved = db.getPengaturan()
        seekMusic.progress = saved.volumeMusik
        seekSfx.progress = saved.volumeSfx
        switchVibration.isChecked = saved.getarAktif

        fun preview() = MenuAudio.setVolumes(seekMusic.progress, seekSfx.progress)
        seekMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) preview()
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
        seekSfx.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) preview()
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) { MenuAudio.button() }
        })
        switchVibration.setOnCheckedChangeListener { _, enabled ->
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (!enabled) vibrator?.cancel()
            else if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(40)
                }
            }
        }
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.rowOther).setOnClickListener {
            startActivity(Intent(this, OtherActivity::class.java))
        }
        findViewById<View>(R.id.btnSave).setOnClickListener {
            db.savePengaturan(Pengaturan(seekMusic.progress, seekSfx.progress, switchVibration.isChecked))
            Toast.makeText(this, "Tersimpan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    override fun onResume() {
        super.onResume()
        // Restore this screen's draft preview after returning from Other settings.
        MenuAudio.setVolumes(seekMusic.progress, seekSfx.progress)
    }
    override fun onPause() {
        // Save commits; Back merely cancels the preview. Neither operation reloads music.
        val saved = db.getPengaturan()
        MenuAudio.setVolumes(saved.volumeMusik, saved.volumeSfx)
        super.onPause()
    }
    override fun onDestroy() { db.close(); super.onDestroy() }
}
