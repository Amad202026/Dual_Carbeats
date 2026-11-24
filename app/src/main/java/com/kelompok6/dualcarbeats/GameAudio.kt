package com.kelompok6.dualcarbeats

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build

/** Owns playback, focus and SoundPool; all calls are on the main thread. */
class GameAudio(private val context: Context, musicVolume: Int, sfxVolume: Int) {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private var musicVol = musicVolume.coerceIn(0, 100) / 100f
    private var sfxVol = sfxVolume.coerceIn(0, 100) / 100f
    private var music: MediaPlayer? = null
    private var released = false
    private var focused = false
    var onInterrupted: (() -> Unit)? = null
    var onCompleted: (() -> Unit)? = null
    var onError: (() -> Unit)? = null
    val positionMs: Long get() = music?.currentPosition?.toLong() ?: 0L
    val durationMs: Long get() = music?.duration?.toLong() ?: 0L
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change < 0) {
            focused = false
            music?.let { if (it.isPlaying) it.pause() }
            soundPool.autoPause()
            onInterrupted?.invoke()
        }
        // No automatic resume: the player explicitly resumes the paused game.
    }
    private val request = if (Build.VERSION.SDK_INT >= 26) AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener(focusListener).build() else null
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pauseMusic(); onInterrupted?.invoke()
            }
        }
    }
    private val soundPool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val sfxIds = mutableMapOf<String, Int>()
    private val loaded = mutableSetOf<Int>()
    private val aliases = mapOf("sfx_hit" to "sfx_poin", "sfx_crash" to "sfx_damage",
        "sfx_gameover" to "sfx_loss", "sfx_countdown" to "sfx_button",
        "sfx_fever" to "sfx_poin")
    init {
        soundPool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) }
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(noisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else context.registerReceiver(noisyReceiver, filter)
    }
    fun loadMusic(rawName: String, loop: Boolean = false) {
        music?.release()
        val id = context.resources.getIdentifier(rawName, "raw", context.packageName)
        require(id != 0) { "Audio tidak ditemukan: $rawName" }
        music = MediaPlayer.create(context, id, attributes, 0)
        requireNotNull(music) { "Audio gagal dimuat: $rawName" }
        music?.apply {
            isLooping = loop
            setVolume(musicVol, musicVol)
            setOnCompletionListener { onCompleted?.invoke() }
            setOnErrorListener { _, _, _ -> onError?.invoke(); true }
        }
    }
    fun setVolumes(musicVolume: Int, sfxVolume: Int) {
        musicVol = musicVolume.coerceIn(0, 100) / 100f
        sfxVol = sfxVolume.coerceIn(0, 100) / 100f
        music?.setVolume(musicVol, musicVol)
    }

    fun loadSfx(names: List<String>) {
        for (name in names) {
            val raw = aliases[name] ?: name
            val id = context.resources.getIdentifier(raw, "raw", context.packageName)
            if (id != 0) sfxIds[name] = soundPool.load(context, id, 1)
        }
    }
    fun playSfx(name: String) {
        if (released || !focused) return
        val id = sfxIds[name] ?: return
        if (id in loaded) soundPool.play(id, sfxVol, sfxVol, 1, 0, if (name == "sfx_fever") 1.25f else 1f)
    }
    @Suppress("DEPRECATION")
    fun acquireFocus(): Boolean {
        if (released) return false
        if (focused) return true
        val result = if (Build.VERSION.SDK_INT >= 26) manager.requestAudioFocus(request!!)
            else manager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        focused = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return focused
    }
    fun startMusic(): Boolean {
        if (!acquireFocus() || music == null) return false
        music?.let { if (!it.isPlaying) it.start() }
        return true
    }
    fun pauseMusic() { music?.let { if (it.isPlaying) it.pause() } }
    fun resumeMusic(): Boolean = startMusic()
    @Suppress("DEPRECATION")
    fun suspendAudio() {
        pauseMusic()
        soundPool.autoPause()
        if (Build.VERSION.SDK_INT >= 26) manager.abandonAudioFocusRequest(request!!)
        else manager.abandonAudioFocus(focusListener)
        focused = false
    }
    fun release() {
        if (released) return
        suspendAudio()
        released = true
        context.unregisterReceiver(noisyReceiver)
        music?.release(); music = null
        soundPool.release()
    }
}

/** One application-context player shared by all menu screens; never holds an Activity. */
object MenuAudio {
    private var player: GameAudio? = null
    private var visibleMenus = 0

    fun onMenuStarted() { visibleMenus++ }
    fun onMenuStopped() {
        visibleMenus = (visibleMenus - 1).coerceAtLeast(0)
        if (visibleMenus == 0) player?.suspendAudio()
    }
    fun enterGame() { player?.suspendAudio() }

    fun resume(context: Context) {
        val db = DatabaseHelper(context.applicationContext)
        val settings = try { db.getPengaturan() } finally { db.close() }
        if (player == null) {
            player = GameAudio(context.applicationContext, settings.volumeMusik, settings.volumeSfx).apply {
                loadMusic("music_bgm", true)
                loadSfx(listOf("sfx_button"))
            }
        }
        setVolumes(settings.volumeMusik, settings.volumeSfx)
        player?.startMusic()
    }
    fun setVolumes(music: Int, sfx: Int) { player?.setVolumes(music, sfx) }
    fun button() { player?.playSfx("sfx_button") }
}
