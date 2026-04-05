package com.megalife.ime.core

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.preference.PreferenceManager

/**
 * Manages haptic feedback and key sounds.
 */
class FeedbackManager(private val context: Context) {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    // Cache prefs to avoid re-reading on every keypress
    private var vibrationEnabled = true
    private var soundEnabled = false
    private var vibrationDuration = 15L  // default medium

    init {
        reloadPrefs()
    }

    fun reloadPrefs() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        vibrationEnabled = prefs.getBoolean("pref_vibration", true)
        soundEnabled = prefs.getBoolean("pref_key_sound", false)
        vibrationDuration = getVibrationDuration(context)
    }

    fun onKeyPress() {
        if (vibrationEnabled && vibrationDuration > 0) vibrate(vibrationDuration)
        if (soundEnabled) playClick()
    }

    fun onSpecialKeyPress() {
        if (vibrationEnabled && vibrationDuration > 0) vibrate(vibrationDuration * 2)
        if (soundEnabled) playClick()
    }

    fun onCapsToggle() {
        if (vibrationEnabled && vibrationDuration > 0) vibrate(vibrationDuration * 2)
    }

    fun onCapsLockReminder() {
        if (vibrationEnabled && vibrationDuration > 0) vibrate(vibrationDuration)
    }

    companion object {
        fun getVibrationDuration(context: Context): Long {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return when (prefs.getString("pref_vibration_strength", "medium")) {
                "off" -> 0L
                "light" -> 5L
                "medium" -> 15L
                "strong" -> 30L
                else -> 15L
            }
        }
    }

    private fun vibrate(durationMs: Long) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    private fun playClick() {
        audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.5f)
    }
}
