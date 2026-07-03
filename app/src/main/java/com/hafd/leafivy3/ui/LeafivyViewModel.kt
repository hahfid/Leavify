package com.hafd.leafivy3.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hafd.leafivy3.ml.DiseaseClassifier
import com.hafd.leafivy3.ml.Prediction
import com.hafd.leafivy3.utils.LocalLogger
import com.hafd.leafivy3.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LeafivyViewModel(application: Application) : AndroidViewModel(application) {

    private var classifier: DiseaseClassifier? = null
    private val _uiState = MutableStateFlow(LeafivyUiState())
    val uiState: StateFlow<LeafivyUiState> = _uiState.asStateFlow()

    /** Simpan gambar untuk ditampilkan di preview, belum diproses */
    fun setPreviewImage(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                pendingImage = bitmap,
                image = null,
                predictions = emptyList(),
                isLoading = false,
                error = null
            )
        }
    }

    /** Proses gambar yang sudah ada di pendingImage */
    fun processImage() {
        val bitmap = _uiState.value.pendingImage ?: return
        val activeClassifier = getOrCreateClassifier()
        if (activeClassifier == null) {
            _uiState.update {
                it.copy(
                    image = bitmap,
                    pendingImage = null,
                    predictions = emptyList(),
                    isLoading = false,
                    error = "Model gagal dimuat di perangkat ini. Coba restart aplikasi."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                image = bitmap,
                pendingImage = null,
                predictions = emptyList(),
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (val result = activeClassifier.classify(bitmap)) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                predictions = result.data,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                predictions = emptyList(),
                                isLoading = false,
                                error = result.message ?: "An error occurred while analyzing the image"
                            )
                        }
                    }
                    is Result.Loading -> {
                        // Already in loading state
                    }
                }
            } catch (e: Exception) {
                LocalLogger.e("LeafivyViewModel", "Unexpected classification failure", e)
                _uiState.update {
                    it.copy(
                        predictions = emptyList(),
                        isLoading = false,
                        error = "An unexpected error occurred. Please try again."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun reset() {
        _uiState.value = LeafivyUiState()
    }

    override fun onCleared() {
        super.onCleared()
        classifier?.close()
    }

    private fun getOrCreateClassifier(): DiseaseClassifier? {
        classifier?.let { return it }

        return try {
            DiseaseClassifier(getApplication()).also { classifier = it }
        } catch (t: Throwable) {
            LocalLogger.e("LeafivyViewModel", "Failed to initialize classifier", t)
            null
        }
    }
}

data class LeafivyUiState(
    val pendingImage: Bitmap? = null,   // gambar di tahap preview, belum diproses
    val image: Bitmap? = null,          // gambar yang sudah/sedang diproses
    val predictions: List<Prediction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
