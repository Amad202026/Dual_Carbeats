package com.kelompok6.dualcarbeats

import android.app.Activity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

fun playResultIntro(activity: Activity) {
    val density = activity.resources.displayMetrics.density

    val rank = activity.findViewById<View>(R.id.tvRank)
    rank.alpha = 0f
    rank.scaleX = 0f
    rank.scaleY = 0f
    rank.post {
        rank.pivotX = rank.width / 2f
        rank.pivotY = rank.height.toFloat()
        rank.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(250)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator(3f))
            .start()
    }

    val valueIds = listOf(
        R.id.tvFinalScore,
        R.id.tvPerfect,
        R.id.tvGreat,
        R.id.tvMiss,
        R.id.tvMaxCombo,
        R.id.tvHighScore
    )
    valueIds.forEachIndexed { index, id ->
        val value = activity.findViewById<View>(id)
        val row = (value.parent?.parent as? View) ?: value
        row.alpha = 0f
        row.translationY = -90 * density
        row.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(900L + index * 150L)
            .setDuration(450)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}
