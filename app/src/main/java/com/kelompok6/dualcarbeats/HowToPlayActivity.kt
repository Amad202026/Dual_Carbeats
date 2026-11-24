package com.kelompok6.dualcarbeats

import android.os.Bundle
import android.view.View

class HowToPlayActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_how_to_play)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }
}
