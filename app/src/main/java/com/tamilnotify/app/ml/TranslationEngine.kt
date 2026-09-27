package com.tamilnotify.app.ml

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class ModelStatus {
    object Checking : ModelStatus()
    object NotDownloaded : ModelStatus()
    object Downloading : ModelStatus()
    object Ready : ModelStatus()
    data class Error(val message: String) : ModelStatus()
}

/**
 * Manages Google ML Kit on-device translation models, downloads, and translation lifecycle.
 * Works completely offline once model weights are stored on device (~30MB per language pair).
 */
class TranslationEngine {

    companion object {
        private const val TAG = "TamilTranslationEngine"

        @Volatile
        private var instance: TranslationEngine? = null

        fun getInstance(): TranslationEngine {
            return instance ?: synchronized(this) {
                instance ?: TranslationEngine().also { instance = it }
            }
        }
    }

    private val mutex = Mutex()
    private var currentTranslator: Translator? = null
    private var currentSourceLang: String = TranslateLanguage.ENGLISH
    private var currentTargetLang: String = TranslateLanguage.TAMIL

    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.Checking)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    private val modelManager = RemoteModelManager.getInstance()

    init {
        // Asynchronously check model download status on initialization
        checkModelStatus(TranslateLanguage.TAMIL)
    }

    /**
     * Checks if the required target model is already downloaded on the device.
     */
    fun checkModelStatus(targetLanguage: String = currentTargetLang) {
        val targetModel = TranslateRemoteModel.Builder(targetLanguage).build()
        modelManager.isModelDownloaded(targetModel)
            .addOnSuccessListener { isDownloaded ->
                _modelStatus.value = if (isDownloaded) {
                    ModelStatus.Ready
                } else {
                    ModelStatus.NotDownloaded
                }
            }
            .addOnFailureListener { ex ->
                _modelStatus.value = ModelStatus.Error(ex.localizedMessage ?: "Status check failed")
            }
    }

    /**
     * Downloads the on-device translation model.
     * @param requireWifi if true, prevents mobile data usage during download.
     */
    suspend fun downloadModel(
        targetLanguage: String = TranslateLanguage.TAMIL,
        requireWifi: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        _modelStatus.value = ModelStatus.Downloading
        Log.i(TAG, "Starting download for language model: $targetLanguage (requireWifi=$requireWifi)")

        val conditionsBuilder = DownloadConditions.Builder()
        if (requireWifi) {
            conditionsBuilder.requireWifi()
        }
        val conditions = conditionsBuilder.build()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLanguage)
            .build()

        val translator = Translation.getClient(options)

        try {
            translator.downloadModelIfNeeded(conditions).await()
            _modelStatus.value = ModelStatus.Ready
            Log.i(TAG, "✓ On-device translation model ($targetLanguage) downloaded successfully!")
            Result.success(Unit)
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: "Failed to download language model"
            Log.e(TAG, "✗ Model download failed: $errMsg", e)
            _modelStatus.value = ModelStatus.Error(errMsg)
            Result.failure(e)
        }
    }

    /**
     * Deletes the target language model from storage to free space if requested.
     */
    suspend fun deleteModel(targetLanguage: String = TranslateLanguage.TAMIL): Result<Unit> = withContext(Dispatchers.IO) {
        val targetModel = TranslateRemoteModel.Builder(targetLanguage).build()
        try {
            modelManager.deleteDownloadedModel(targetModel).await()
            mutex.withLock {
                currentTranslator?.close()
                currentTranslator = null
            }
            _modelStatus.value = ModelStatus.NotDownloaded
            Log.i(TAG, "Deleted model: $targetLanguage")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Translates English text into the target language completely on-device.
     */
    suspend fun translate(
        text: String,
        targetLanguage: String = TranslateLanguage.TAMIL
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            return@withContext Result.success("")
        }

        try {
            val translator = getOrCreateTranslator(targetLanguage)
            val translated = translator.translate(text).await()
            Result.success(translated)
        } catch (e: Exception) {
            Log.e(TAG, "Translation error for text [$text]: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun getOrCreateTranslator(targetLanguage: String): Translator {
        mutex.withLock {
            if (currentTranslator != null && currentTargetLang == targetLanguage) {
                return currentTranslator!!
            }

            // Close existing translator before opening a new language pair
            currentTranslator?.close()

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLanguage)
                .build()

            currentTargetLang = targetLanguage
            val translator = Translation.getClient(options)
            currentTranslator = translator
            return translator
        }
    }
}
