package com.crischarles.bitlog

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val SAMPLE_RATE = 22050
private const val CYCLE_SECONDS = 2.0

/** Strike start times inside one cycle, in seconds. */
private val STRIKE_STARTS = doubleArrayOf(0.0, 0.6)

/** One bell strike at [dt] seconds after it begins: a fundamental, two overtones and a fast decay. */
private fun bellSample(dt: Double): Double {
    val wave = sin(2 * PI * 1318.5 * dt) +
        0.5 * sin(2 * PI * 1760.0 * dt) +
        0.25 * sin(2 * PI * 2637.0 * dt)
    return wave / 1.75 * exp(-dt * 4.0)
}

/**
 * Returns the built-in bell ringtone as a looping WAV file in the cache folder.
 * It is generated once and reused.
 */
fun builtInRingtoneFile(context: Context): File {
    val file = File(context.cacheDir, "egg_bell_v1.wav")
    if (file.exists() && file.length() > 44) return file

    val frames = (SAMPLE_RATE * CYCLE_SECONDS).toInt()
    val pcmLength = frames * 2
    val out = ByteBuffer.allocate(44 + pcmLength).order(ByteOrder.LITTLE_ENDIAN)

    out.put("RIFF".toByteArray())
    out.putInt(36 + pcmLength)
    out.put("WAVE".toByteArray())
    out.put("fmt ".toByteArray())
    out.putInt(16)
    out.putShort(1) // PCM
    out.putShort(1) // mono
    out.putInt(SAMPLE_RATE)
    out.putInt(SAMPLE_RATE * 2)
    out.putShort(2)
    out.putShort(16)
    out.put("data".toByteArray())
    out.putInt(pcmLength)

    for (i in 0 until frames) {
        val t = i / SAMPLE_RATE.toDouble()
        var value = 0.0
        for (start in STRIKE_STARTS) {
            val dt = t - start
            if (dt >= 0) value += bellSample(dt)
        }
        val sample = (value.coerceIn(-1.0, 1.0) * 0.95 * Short.MAX_VALUE).toInt()
        out.putShort(sample.toShort())
    }

    file.writeBytes(out.array())
    return file
}

/**
 * Plays the ringtone on a loop at the alarm stream's maximum volume.
 * Plays [customUri] when given, otherwise the built-in bell.
 */
class RingPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    fun play(customUri: String?) {
        stop()
        try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0,
            )
        } catch (_: Exception) {}

        val started = customUri != null &&
            startWith { it.setDataSource(context, Uri.parse(customUri)) }
        if (!started) {
            startWith { it.setDataSource(builtInRingtoneFile(context).absolutePath) }
        }
    }

    private fun startWith(setSource: (MediaPlayer) -> Unit): Boolean {
        return try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setSource(mp)
            mp.isLooping = true
            mp.setVolume(1f, 1f)
            mp.prepare()
            mp.start()
            player = mp
            true
        } catch (e: Exception) {
            false
        }
    }

    fun stop() {
        player?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        player = null
    }
}
