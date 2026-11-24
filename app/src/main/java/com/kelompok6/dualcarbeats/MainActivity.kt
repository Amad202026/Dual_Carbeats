package com.kelompok6.dualcarbeats

import android.content.Intent
import android.os.Bundle
import android.view.View

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.btnPlay).setOnClickListener {
            startActivity(Intent(this, SelectMapActivity::class.java))
        }
        findViewById<View>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, HowToPlayActivity::class.java))
        }
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.btnInfo).setOnClickListener {
            startActivity(Intent(this, CreditActivity::class.java))
        }
    }
}
