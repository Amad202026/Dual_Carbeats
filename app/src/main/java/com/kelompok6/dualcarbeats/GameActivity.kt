package com.kelompok6.dualcarbeats

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import java.util.Locale

class GameActivity : BaseActivity(), GameListener {

    private lateinit var db: DatabaseHelper
    private lateinit var audio: GameAudio
    private lateinit var engine: GameEngine
    private var events: List<MapEvent> = emptyList()
    private var levelId = 1

    private lateinit var gameArea: LinearLayout
    private lateinit var lanes: List<FrameLayout>
    private lateinit var fx: List<ImageView>
    private lateinit var hearts: List<ImageView>
    private lateinit var ivCarRed: ImageView
    private lateinit var ivCarBlue: ImageView
    private lateinit var tvScore: TextView
    private lateinit var tvCombo: TextView
    private lateinit var tvComboLabel: TextView
    private lateinit var ivJudgement: ImageView
    private lateinit var ivFever: ImageView
    private lateinit var ivFeverLeft: ImageView
    private lateinit var ivFeverRight: ImageView
    private lateinit var pauseOverlay: View

    private val main = Handler(Looper.getMainLooper())

    private var laneW = 0
    private var laneH = 0
    private var hitY = 0f
    private var approachMs = 1800L
    private var pxPerMs = 0f

    private var offsetMs = 0L
    private var vibrateOn = true

    private var started = false
    private var paused = false
    private var ended = false
    private var foreground = false
    private var countdownRunning = false

    private var spawnIdx = 0
    private val active = ArrayList<MapEvent>()
    private val pointerCar = HashMap<Int, Car>()
    private lateinit var road: ScrollingRoadDrawable
    private val carGlows = mutableMapOf<Car, ImageView>()
    private val fxFollowers = mutableMapOf<Int, Car>()
    private fun dp(value: Float) = value * resources.displayMetrics.density

    private val hideFeverText = Runnable { ivFever.visibility = View.GONE }
    private val hideJudgement = Runnable { ivJudgement.visibility = View.GONE }

    private fun songTime(): Long =
        audio.positionMs + offsetMs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        db = DatabaseHelper(this)
        levelId = intent.getIntExtra(Extras.LEVEL_ID, 1)
        val level = db.getLevel(levelId)
        val settings = db.getPengaturan()
        vibrateOn = settings.getarAktif
        offsetMs = GamePrefs.getOffset(this).toLong()
        approachMs = 2400L - (GamePrefs.getSpeed(this).coerceIn(1, 10) - 1) * 150L

        bindViews()

        audio = GameAudio(this, settings.volumeMusik, settings.volumeSfx)
        audio.onInterrupted = { if (started) pauseGame() else { cancelCountdown(); finish() } }
        audio.onCompleted = { if (!ended) engine.onSongCompleted(audio.durationMs + kotlin.math.abs(offsetMs) + GameEngine.GREAT_MS + 1) }
        audio.onError = {
            Toast.makeText(this, "Audio gagal diputar. Coba lagi.", Toast.LENGTH_LONG).show()
            finish()
        }
        audio.loadSfx(listOf("sfx_hit", "sfx_miss", "sfx_crash", "sfx_fever", "sfx_win", "sfx_gameover", "sfx_countdown", "sfx_button"))
        try {
            requireNotNull(level) { "Level tidak ditemukan" }
            audio.loadMusic(level.fileMusik)
            events = MapLoader.load(this, level.fileMap)
            require(events.all { it.t + it.duration + GameEngine.GREAT_MS + kotlin.math.abs(offsetMs) < audio.durationMs }) { "Map melewati akhir lagu" }
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Map/audio tidak valid", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        engine = GameEngine(events, this)

        setupButtons()
        setupTouch()

        onStatsChanged(0, 0)
        onHpChanged(GameEngine.MAX_HP)

        gameArea.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateGeometry()
        }
        gameArea.post { updateGeometry(); startCountdown() }
    }

    private fun updateGeometry() {
        if (lanes[0].width == 0 || ivCarRed.height == 0) return
        laneW = lanes[0].width
        laneH = lanes[0].height
        val carCenterY = laneH * .80f
        for (car in listOf(ivCarRed, ivCarBlue)) {
            car.translationY = carCenterY - car.top - car.height / 2f
        }
        // Musical target sits just ahead of the bumper, not far up the road.
        val carFront = ivCarRed.y
        val largestHalfObject = laneW * .74f / 2f
        hitY = (carFront - largestHalfObject - dp(4f)).coerceAtLeast(dp(80f))
        pxPerMs = hitY / approachMs
        // Feedback lives in the middle of the road, independently of car height.
        val judgementParams = ivJudgement.layoutParams as FrameLayout.LayoutParams
        judgementParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        judgementParams.topMargin = (laneH * .43f - ivJudgement.layoutParams.height / 2f -
            findViewById<View>(R.id.gameHud).paddingTop).toInt().coerceAtLeast(0)
        judgementParams.bottomMargin = 0
        ivJudgement.layoutParams = judgementParams
        updateCarDecorations()
    }

    private fun updateCarDecorations() {
        for ((car, glow) in carGlows) {
            val body = if (car == Car.RED) ivCarRed else ivCarBlue
            glow.translationX = body.translationX
            glow.translationY = body.y + body.height / 2f - glow.layoutParams.height / 2f
        }
        for ((lane, car) in fxFollowers) {
            val body = if (car == Car.RED) ivCarRed else ivCarBlue
            val effect = fx[lane - 1]
            val baseLane = if (car == Car.RED) 2 else 4
            effect.translationX = (baseLane - lane) * laneW + body.translationX
            effect.translationY = body.y + body.height / 2f - effect.layoutParams.height / 2f
        }
    }

    private fun bindViews() {
        gameArea = findViewById(R.id.gameArea)
        lanes = listOf(R.id.lane1, R.id.lane2, R.id.lane3, R.id.lane4).map { findViewById<FrameLayout>(it) }
        fx = listOf(R.id.ivFx1, R.id.ivFx2, R.id.ivFx3, R.id.ivFx4).map { findViewById<ImageView>(it) }
        hearts = listOf(R.id.ivHeart1, R.id.ivHeart2, R.id.ivHeart3).map { findViewById<ImageView>(it) }
        ivCarRed = findViewById(R.id.ivCarRed)
        ivCarBlue = findViewById(R.id.ivCarBlue)
        tvScore = findViewById(R.id.tvScore)
        tvCombo = findViewById(R.id.tvCombo)
        tvComboLabel = findViewById(R.id.tvComboLabel)
        ivJudgement = findViewById(R.id.ivJudgement)
        ivFever = findViewById(R.id.ivFever)
        ivFeverLeft = findViewById(R.id.ivFeverLeft)
        ivFeverRight = findViewById(R.id.ivFeverRight)
        pauseOverlay = findViewById(R.id.pauseOverlay)

        gameArea.clipChildren = false
        lanes.forEach { it.clipChildren = false }
        val roadBitmap = requireNotNull(BitmapFactory.decodeResource(resources, R.drawable.road,
            BitmapFactory.Options().apply { inScaled = false; inSampleSize = 2 }))
        road = ScrollingRoadDrawable(roadBitmap, resources.displayMetrics.density)
        gameArea.background = road
        for (car in Car.values()) {
            val baseLane = if (car == Car.RED) 2 else 4
            val halo = ImageView(this).apply {
                setImageDrawable(NeonHaloDrawable(if (car == Car.RED) 0xFFFF4269.toInt() else 0xFF00EEFF.toInt()))
                scaleType = ImageView.ScaleType.FIT_XY
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            lanes[baseLane - 1].addView(halo, 0, FrameLayout.LayoutParams(
                dp(86f).toInt(), dp(132f).toInt(), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
            carGlows[car] = halo
        }
        fx.forEach {
            it.layoutParams = FrameLayout.LayoutParams(dp(100f).toInt(), dp(100f).toInt(),
                Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        }
    }

    private fun setupButtons() {
        findViewById<View>(R.id.btnPause).setOnClickListener { audio.playSfx("sfx_button"); if (started && !ended) pauseGame() }
        findViewById<View>(R.id.btnResume).setOnClickListener { resumeGame(); audio.playSfx("sfx_button") }
        findViewById<View>(R.id.btnPauseRetry).setOnClickListener { recreate() }
        findViewById<View>(R.id.btnPauseHome).setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        onBackPressedDispatcher.addCallback(this) {
            when {
                !started -> finish()
                paused -> resumeGame()
                else -> pauseGame()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouch() {
        gameArea.setOnTouchListener { v, e ->
            if (!started || paused || ended) return@setOnTouchListener true
            val time = songTime()
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val i = e.actionIndex
                    val car = if (e.getX(i) < v.width / 2f) Car.RED else Car.BLUE
                    if (!pointerCar.containsValue(car)) {
                        pointerCar[e.getPointerId(i)] = car
                        engine.onTouchDown(car, time)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    val car = pointerCar.remove(e.getPointerId(e.actionIndex))
                    if (car != null) engine.onTouchUp(car, time)
                }
                MotionEvent.ACTION_CANCEL -> {
                    pointerCar.values.forEach { engine.onTouchUp(it, time) }
                    pointerCar.clear()
                }
            }
            true
        }
    }

    private fun cancelCountdown() {
        main.removeCallbacksAndMessages(null)
        countdownRunning = false
    }

    private fun startCountdown() {
        if (!foreground || started || countdownRunning || laneW == 0) return
        if (!audio.acquireFocus()) {
            Toast.makeText(this, "Audio sedang digunakan. Coba lagi.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        countdownRunning = true
        tvCombo.visibility = View.VISIBLE
        tvComboLabel.visibility = View.INVISIBLE
        for (i in 0..2) {
            main.postDelayed({
                tvCombo.text = (3 - i).toString()
                audio.playSfx("sfx_countdown")
            }, i * 1000L)
        }
        main.postDelayed({ beginGame() }, 3000L)
    }

    private fun beginGame() {
        countdownRunning = false
        if (!foreground || !audio.startMusic()) { finish(); return }
        tvComboLabel.visibility = View.VISIBLE
        onStatsChanged(0, 0)
        started = true
        Choreographer.getInstance().postFrameCallback(frame)
    }

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!started || paused || ended) return
            val time = songTime()
            engine.update(time)
            if (ended) return
            spawn(time)
            updateViews(time)
            road.advance(time)
            updateCarDecorations()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun spawn(time: Long) {
        while (spawnIdx < events.size && events[spawnIdx].t - approachMs <= time) {
            createViews(events[spawnIdx])
            active.add(events[spawnIdx])
            spawnIdx++
        }
    }

    private fun createViews(ev: MapEvent) {
        val lane = lanes[ev.lane - 1]
        val size = (laneW * 0.74f).toInt()
        val red = ev.car == Car.RED
        val list = ArrayList<View>()

        fun add(res: Int, w: Int, h: Int, stretch: Boolean = false) {
            val iv = ImageView(this)
            iv.setImageResource(res)
            iv.scaleType = if (stretch) ImageView.ScaleType.FIT_XY else ImageView.ScaleType.FIT_CENTER
            lane.addView(iv, FrameLayout.LayoutParams(w, h, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
            iv.translationY = -10000f
            list.add(iv)
        }

        val noteRes = if (red) R.drawable.red_not else R.drawable.blue_not
        when (ev.type) {
            NoteType.NOTE -> add(noteRes, size, size)
            NoteType.OBSTACLE -> {
                val w = (laneW * 0.64f).toInt()
                add(R.drawable.obstacle, w, (w * 58f / 66f).toInt())
            }
            NoteType.HOLD -> {
                val tailH = (ev.duration * pxPerMs).toInt().coerceAtLeast(1)
                add(if (red) R.drawable.red_tail else R.drawable.blue_tail, size, tailH, true)
                add(noteRes, size, size)
                add(noteRes, size, size)
            }
        }
        ev.views = list

        ivCarRed.bringToFront()
        ivCarBlue.bringToFront()
    }

    private fun updateViews(time: Long) {
        val iter = active.iterator()
        while (iter.hasNext()) {
            val ev = iter.next()
            if (positionViews(ev, time)) {
                ev.views.forEach { (it.parent as? ViewGroup)?.removeView(it) }
                iter.remove()
            }
        }
    }

    private fun objectY(targetTime: Long, time: Long): Float {
        // Constant song-clock velocity before and after the hit time: no dwell or slowdown.
        return hitY + (time - targetTime) * pxPerMs
    }

    private fun positionViews(ev: MapEvent, time: Long): Boolean {
        val yHead = objectY(ev.t, time)
        val v = ev.views

        return when (ev.type) {
            NoteType.NOTE -> {
                val h = v[0].layoutParams.height
                v[0].translationY = yHead - h / 2f
                ev.state == MapEvent.HIT || yHead - h / 2f > laneH
            }
            NoteType.OBSTACLE -> {
                val h = v[0].layoutParams.height
                v[0].translationY = yHead - h / 2f
                ev.state != MapEvent.PENDING || yHead - h / 2f > laneH
            }
            NoteType.HOLD -> {
                val tail = v[0]
                val head = v[1]
                val end = v[2]
                val size = head.layoutParams.height
                val tailH = tail.layoutParams.height
                val yEnd = objectY(ev.t + ev.duration, time)
                val holdingNow = ev.state == MapEvent.HIT

                tail.translationY = yEnd
                head.translationY = (if (holdingNow) minOf(yHead, hitY) else yHead) - size / 2f
                end.translationY = yEnd - size / 2f
                end.visibility = if (holdingNow && yEnd > hitY) View.INVISIBLE else View.VISIBLE

                tail.pivotY = 0f
                tail.scaleY = ((if (holdingNow) minOf(yHead, hitY) else yHead) - yEnd)
                    .coerceAtLeast(0f) / tailH

                ev.state == MapEvent.FAILED ||
                    (holdingNow && time > ev.t + ev.duration) ||
                    yEnd - size / 2f > laneH
            }
        }
    }

    private fun pauseGame() {
        if (!started || paused || ended) return
        paused = true
        Choreographer.getInstance().removeFrameCallback(frame)
        audio.pauseMusic()

        val time = songTime()
        pointerCar.values.forEach { engine.onTouchUp(it, time) }
        pointerCar.clear()

        pauseOverlay.visibility = View.VISIBLE
    }

    private fun resumeGame() {
        if (!paused || !foreground || ended) return
        if (!audio.resumeMusic()) {
            Toast.makeText(this, "Audio belum tersedia", Toast.LENGTH_SHORT).show()
            return
        }
        paused = false
        pauseOverlay.visibility = View.GONE
        Choreographer.getInstance().postFrameCallback(frame)
    }

    override fun onHoldStart(event: MapEvent) {
        audio.playSfx("sfx_hit")
        playFx(event.targetLane, R.drawable.fx_note, event.car)
    }

    override fun onJudgement(judgement: Judgement, event: MapEvent) {
        val res = when (judgement) {
            Judgement.PERFECT -> R.drawable.perfect
            Judgement.GREAT -> R.drawable.great
            Judgement.MISS -> R.drawable.miss
        }
        showJudgement(res)
        if (judgement == Judgement.MISS) {
            audio.playSfx("sfx_miss")
        } else {
            audio.playSfx("sfx_hit")
            playFx(event.targetLane, R.drawable.fx_note, event.car)
        }
    }

    override fun onCarMove(car: Car, toLane: Int) {
        val view = if (car == Car.RED) ivCarRed else ivCarBlue
        val baseLane = if (car == Car.RED) 2 else 4
        view.animate().translationX(((toLane - baseLane) * laneW).toFloat()).setDuration(100).start()
    }

    override fun onCrash(event: MapEvent) {
        playFx(event.lane, R.drawable.fx_crash, event.car)
        audio.playSfx("sfx_crash")
        vibrate()
    }

    override fun onStatsChanged(score: Int, combo: Int) {
        tvScore.text = String.format(Locale.US, "%07d", score)
        tvCombo.text = combo.toString()
        tvCombo.visibility = if (combo > 0) View.VISIBLE else View.INVISIBLE
        tvComboLabel.visibility = if (combo > 0) View.VISIBLE else View.INVISIBLE
    }

    override fun onHpChanged(hp: Int) {
        for (i in 0..2) {
            hearts[i].setImageResource(if (i >= GameEngine.MAX_HP - hp) R.drawable.heart else R.drawable.heart_empty)
        }
    }

    override fun onFeverStart() {
        audio.playSfx("sfx_fever")
        startAnimation(ivFeverLeft, R.drawable.fx_fever)
        startAnimation(ivFeverRight, R.drawable.fx_fever)
        ivFeverLeft.visibility = View.VISIBLE
        ivFeverRight.visibility = View.VISIBLE
        ivFever.visibility = View.VISIBLE
        ivFever.removeCallbacks(hideFeverText)
        ivFever.postDelayed(hideFeverText, 1500)
    }

    override fun onFeverEnd() {
        ivFeverLeft.visibility = View.GONE
        ivFeverRight.visibility = View.GONE
    }

    override fun onInvalidTap(car: Car) {
        showJudgement(R.drawable.miss)
        audio.playSfx("sfx_miss")
    }

    override fun onGameEnd(win: Boolean) {
        ended = true
        if (win && engine.fullCombo) db.recordFullCombo(levelId)
        Choreographer.getInstance().removeFrameCallback(frame)
        audio.pauseMusic()
        audio.playSfx(if (win) "sfx_win" else "sfx_gameover")
        main.postDelayed({ goToResult(win) }, 1200)
    }

    private fun showJudgement(res: Int) {
        ivJudgement.animate().cancel()
        ivJudgement.scaleX = 1.18f
        ivJudgement.scaleY = 1.18f
        ivJudgement.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        ivJudgement.setImageResource(res)
        ivJudgement.visibility = View.VISIBLE
        ivJudgement.removeCallbacks(hideJudgement)
        ivJudgement.postDelayed(hideJudgement, 400)
    }

    private fun startAnimation(view: ImageView, res: Int) {
        (view.drawable as? AnimationDrawable)?.stop()
        view.setImageResource(res)
        (view.drawable as? AnimationDrawable)?.start()
    }

    private fun playFx(lane: Int, anim: Int, car: Car) {
        fxFollowers[lane] = car
        updateCarDecorations()
        val iv = fx[lane - 1]
        (iv.tag as? Runnable)?.let { iv.removeCallbacks(it) }
        iv.visibility = View.VISIBLE
        iv.bringToFront()
        startAnimation(iv, anim)
        val hide = Runnable { iv.visibility = View.GONE }
        iv.tag = hide
        iv.postDelayed(hide, 500)
    }

    private fun vibrate() {
        if (!vibrateOn) return
        try {
            val vib = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= 26) {
                vib.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(80)
            }
        } catch (e: Exception) {
        }
    }

    private fun goToResult(win: Boolean) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(Extras.LEVEL_ID, levelId)
            putExtra(Extras.WIN, win)
            putExtra(Extras.SCORE, engine.score)
            putExtra(Extras.PERFECT, engine.perfect)
            putExtra(Extras.GREAT, engine.great)
            putExtra(Extras.MISS, engine.miss)
            putExtra(Extras.MAX_COMBO, engine.maxCombo)
        }
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        if (::engine.isInitialized && !started) startCountdown()
        if (ended) main.post { goToResult(engine.hp > 0) }
    }

    override fun onPause() {
        foreground = false
        if (started && !ended && !paused) pauseGame()
        main.removeCallbacksAndMessages(null)
        countdownRunning = false
        if (::audio.isInitialized) audio.suspendAudio()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        main.removeCallbacksAndMessages(null)
        Choreographer.getInstance().removeFrameCallback(frame)
        if (::audio.isInitialized) audio.release()
        if (::db.isInitialized) db.close()
    }
}
