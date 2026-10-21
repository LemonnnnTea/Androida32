package com.example.myapplication

import android.content.Context
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var game: GameViewModel
    private lateinit var audio: GameAudio

    override fun attachBaseContext(newBase: Context) {
        // The assignment's UI is English-only, including Material accessibility labels.
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(Locale.ENGLISH)
            setLayoutDirection(Locale.ENGLISH)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        game = ViewModelProvider(this)[GameViewModel::class.java]
        audio = GameAudio(applicationContext)
        setContent { ForestApp(game, audio) }
    }

    override fun onResume() {
        super.onResume()
        audio.open()
    }

    override fun onPause() {
        game.pause()
        audio.close() // Release native audio resources whenever another app takes over.
        super.onPause()
    }
}

// SoundPool is an Android platform API (Lecture 26), not a third-party sound library.
// All short WAV effects in res/raw were synthesized for this project.
class GameAudio(private val context: Context) {
    private var pool: SoundPool? = null
    private val samples = mutableMapOf<String, Int>()
    private val ready = mutableSetOf<Int>()
    val active: Boolean get() = pool != null

    fun open() {
        if (pool != null) return
        val soundPool = SoundPool.Builder().setMaxStreams(4)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
        pool = soundPool
        soundPool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) }
        samples["pop"] = soundPool.load(context, R.raw.pop, 1)
        samples["combo"] = soundPool.load(context, R.raw.combo, 1)
        samples["win"] = soundPool.load(context, R.raw.win, 1)
        samples["invalid"] = soundPool.load(context, R.raw.invalid, 1)
        samples["lose"] = samples.getValue("invalid")
    }

    fun play(cue: String, chain: Int) {
        val id = samples[cue] ?: return
        if (id in ready) pool?.play(id, .45f, .45f, 1, 0,
            if (cue == "combo") (1f + chain.coerceAtMost(6) * .065f) else 1f)
    }

    fun close() {
        pool?.release()
        pool = null
        samples.clear()
        ready.clear()
    }
}
