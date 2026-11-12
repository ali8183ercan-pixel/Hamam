package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SoundEngine {
  @Volatile var enabled: Boolean = true

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val sampleRate = 22050

  private val coinPcm: ShortArray by lazy {
    generateDualTone(freq1 = 987.77, freq2 = 1318.51, durationMs = 110, volume = 0.25)
  }
  private val jumpPcm: ShortArray by lazy {
    generateSweep(startFreq = 260.0, endFreq = 680.0, durationMs = 130, volume = 0.24)
  }
  private val slidePcm: ShortArray by lazy {
    generateSweep(startFreq = 460.0, endFreq = 160.0, durationMs = 120, volume = 0.22)
  }
  private val lanePcm: ShortArray by lazy {
    generateSweep(startFreq = 320.0, endFreq = 440.0, durationMs = 55, volume = 0.16)
  }
  private val powerUpPcm: ShortArray by lazy {
    generateArpeggio(listOf(523.25, 659.25, 783.99, 1046.50), stepMs = 65, volume = 0.28)
  }
  private val hoverboardPcm: ShortArray by lazy {
    generateSweep(startFreq = 300.0, endFreq = 1100.0, durationMs = 220, volume = 0.28)
  }
  private val stumblePcm: ShortArray by lazy {
    generateSweep(startFreq = 190.0, endFreq = 75.0, durationMs = 180, volume = 0.32)
  }
  private val crashPcm: ShortArray by lazy {
    generateNoiseBurst(durationMs = 260, volume = 0.35)
  }

  fun playCoin() = playAsync(coinPcm)
  fun playJump() = playAsync(jumpPcm)
  fun playSlide() = playAsync(slidePcm)
  fun playLaneSwitch() = playAsync(lanePcm)
  fun playPowerUp() = playAsync(powerUpPcm)
  fun playHoverboard() = playAsync(hoverboardPcm)
  fun playStumble() = playAsync(stumblePcm)
  fun playCrash() = playAsync(crashPcm)

  private fun playAsync(samples: ShortArray) {
    if (!enabled) return
    scope.launch {
      runCatching {
        val byteCount = samples.size * 2
        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setTransferMode(AudioTrack.MODE_STATIC)
          .setBufferSizeInBytes(byteCount)
          .build()

        track.write(samples, 0, samples.size)
        track.setNotificationMarkerPosition(samples.size)
        track.setPlaybackPositionUpdateListener(
          object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack?) {
              runCatching {
                t?.stop()
                t?.release()
              }
            }

            override fun onPeriodicNotification(t: AudioTrack?) = Unit
          }
        )
        track.play()
      }
    }
  }

  private fun generateDualTone(
    freq1: Double,
    freq2: Double,
    durationMs: Int,
    volume: Double,
  ): ShortArray {
    val count = (sampleRate * durationMs) / 1000
    val half = count / 2
    return ShortArray(count) { i ->
      val freq = if (i < half) freq1 else freq2
      val env = 1.0 - (i.toDouble() / count) * 0.65
      val sample = sin(2.0 * PI * freq * i / sampleRate) * volume * env
      (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
  }

  private fun generateSweep(
    startFreq: Double,
    endFreq: Double,
    durationMs: Int,
    volume: Double,
  ): ShortArray {
    val count = (sampleRate * durationMs) / 1000
    var phase = 0.0
    return ShortArray(count) { i ->
      val t = i.toDouble() / count
      val freq = startFreq + (endFreq - startFreq) * t
      phase += 2.0 * PI * freq / sampleRate
      val env = (1.0 - t) * (if (i < 120) i / 120.0 else 1.0)
      val sample = sin(phase) * volume * env
      (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
  }

  private fun generateArpeggio(
    freqs: List<Double>,
    stepMs: Int,
    volume: Double,
  ): ShortArray {
    val stepSamples = (sampleRate * stepMs) / 1000
    val total = stepSamples * freqs.size
    return ShortArray(total) { i ->
      val stepIdx = (i / stepSamples).coerceAtMost(freqs.lastIndex)
      val localT = (i % stepSamples).toDouble() / stepSamples
      val env = 1.0 - localT * 0.45
      val sample = sin(2.0 * PI * freqs[stepIdx] * i / sampleRate) * volume * env
      (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
  }

  private fun generateNoiseBurst(durationMs: Int, volume: Double): ShortArray {
    val count = (sampleRate * durationMs) / 1000
    val rng = Random(42)
    return ShortArray(count) { i ->
      val t = i.toDouble() / count
      val env = (1.0 - t) * (1.0 - t)
      val lowTone = sin(2.0 * PI * 95.0 * i / sampleRate) * 0.5
      val noise = (rng.nextDouble() * 2.0 - 1.0) * 0.5
      val sample = (lowTone + noise) * volume * env
      (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
  }
}
