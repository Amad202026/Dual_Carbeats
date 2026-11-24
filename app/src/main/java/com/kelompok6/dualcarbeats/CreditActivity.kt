package com.kelompok6.dualcarbeats

import android.os.Bundle
import android.view.View

class CreditActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credit)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }
}
