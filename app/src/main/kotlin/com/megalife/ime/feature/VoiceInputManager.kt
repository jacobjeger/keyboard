package com.megalife.ime.feature

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.microsoft.cognitiveservices.speech.*
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.megalife.ime.security.SecureStorage
import kotlinx.coroutines.*

/**
 * Voice input using Azure Speech API.
 * Supports: English, Hebrew, Arabic, Yiddish, Russian, French, Amharic.
 * Only network call in the entire app — isolated to mic button.
 */
class VoiceInputManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val secureStorage = SecureStorage.getInstance(context)

    private val lock = Any()
    private var recognizer: SpeechRecognizer? = null
    @Volatile private var isListening = false

    var onResult: ((String) -> Unit)? = null
    var onListeningChanged: ((Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val languageLocaleMap = mapOf(
        "en" to "en-US",
        "he" to "he-IL",
        "ar" to "ar-SA",
        "yi" to "yi",
        "ru" to "ru-RU",
        "fr" to "fr-FR",
        "am" to "am-ET"
    )

    /** Check if mic permission is granted */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Check if internet is available */
    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Start voice recognition */
    fun startListening(languageCode: String) {
        if (!hasPermission()) {
            onError?.invoke("Microphone permission required")
            return
        }

        if (!isOnline()) {
            Toast.makeText(context, "Voice input requires internet connection", Toast.LENGTH_SHORT).show()
            onError?.invoke("No internet")
            return
        }

        val azureKey = getAzureKey()
        if (azureKey.isEmpty()) {
            onError?.invoke("Azure Speech API key not configured")
            return
        }

        val locale = languageLocaleMap[languageCode] ?: "en-US"

        scope.launch(Dispatchers.IO) {
            try {
                val speechConfig = SpeechConfig.fromSubscription(azureKey, getAzureRegion())
                speechConfig.speechRecognitionLanguage = locale

                val audioConfig = AudioConfig.fromDefaultMicrophoneInput()
                val rec = SpeechRecognizer(speechConfig, audioConfig)
                synchronized(lock) {
                    recognizer = rec
                }

                isListening = true
                withContext(Dispatchers.Main) {
                    onListeningChanged?.invoke(true)
                }

                val result = recognizer?.recognizeOnceAsync()?.get()

                withContext(Dispatchers.Main) {
                    isListening = false
                    onListeningChanged?.invoke(false)

                    handleRecognitionResult(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isListening = false
                    onListeningChanged?.invoke(false)
                    onError?.invoke("Voice error: ${e.message}")
                }
            }
        }
    }

    private fun handleRecognitionResult(result: SpeechRecognitionResult?) {
        val reason = result?.reason
        if (reason == ResultReason.RecognizedSpeech) {
            val text = result.text.orEmpty()
            if (text.isNotEmpty()) {
                onResult?.invoke(text)
            }
        } else if (reason == ResultReason.NoMatch) {
            onError?.invoke("Could not recognize speech")
        } else if (reason == ResultReason.Canceled) {
            val cancellation = CancellationDetails.fromResult(result)
            onError?.invoke("Voice cancelled: ${cancellation.reason}")
        } else {
            onError?.invoke("Voice input failed")
        }
    }

    /** Stop current recognition */
    fun stopListening() {
        synchronized(lock) {
            recognizer?.close()
            recognizer = null
        }
        isListening = false
        onListeningChanged?.invoke(false)
    }

    fun isCurrentlyListening(): Boolean = isListening

    private fun getAzureKey(): String {
        return secureStorage.getAzureKey()
    }

    private fun getAzureRegion(): String {
        return secureStorage.getAzureRegion()
    }

    fun destroy() {
        synchronized(lock) {
            recognizer?.close()
            recognizer = null
        }
    }
}
