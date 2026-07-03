package com.hafd.leafivy3.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import com.hafd.leafivy3.utils.LocalLogger
import com.hafd.leafivy3.utils.Result
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class DiseaseClassifier(
    private val context: Context,
    private val labelPath: String = "labels.txt",
    private val modelFileName: String = "latestmodel2.tflite",
    private val threshold: Float = 0.1f
) {
    private val logTag = "DiseaseClassifier"

    // ── Label ──────────────────────────────────────────────────────────────
    // Urutan index sesuai label_map.json hasil export notebook v3 (Section 11):
    //   TARGET_LABELS = ["scab", "healthy", "rust", "frog_eye_leaf_spot", "powdery_mildew"]
    //   → {"0":"scab","1":"healthy","2":"rust","3":"frog_eye_leaf_spot","4":"powdery_mildew"}
    private val fallbackLabels = listOf(
        "scab",               // index 0
        "healthy",            // index 1
        "rust",               // index 2
        "frog_eye_leaf_spot", // index 3
        "powdery_mildew"      // index 4
    )
    private val expectedNumClasses = 5

    // ── Input size (IMG_SIZE notebook v3, Section 2 Hyperparameter) ────────
    // IMG_SIZE = 512
    private val defaultInputSize = 512

    // ── State yang di-cache ────────────────────────────────────────────────
    private var labels: List<String> = emptyList()
    private var interpreter: Interpreter? = null
    private var runtime: ModelRuntime? = null
    private var modelVersion: String? = null

    // ══════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Klasifikasikan satu bitmap.
     * Interpreter di-lazy-init pada pemanggilan pertama dan di-cache sesudahnya.
     */
    fun classify(bitmap: Bitmap): Result<List<Prediction>> {
        val labels = ensureLabels()
        if (labels.isEmpty()) {
            return Result.Error(Exception("No labels"), "No labels found for disease detection.")
        }

        val (interp, rt) = try {
            ensureInterpreter()
        } catch (e: Exception) {
            LocalLogger.e(logTag, "Failed to load model $modelFileName", e)
            return Result.Error(e, "Failed to load model: ${e.message}")
        }

        if (rt.outputClasses != labels.size) {
            val msg = "Model output classes ${rt.outputClasses} != labels ${labels.size}"
            LocalLogger.e(logTag, msg)
            return Result.Error(Exception(msg), msg)
        }

        return try {
            val scores = runInference(interp, bitmap, rt)

            LocalLogger.d(logTag, "Raw scores: ${scores.mapIndexed { i, s ->
                "${labels.getOrElse(i) { i.toString() }}=${"%.3f".format(s)}"
            }.joinToString()}")

            val allSorted = scores
                .mapIndexed { i, score -> Prediction(labels[i], score) }
                .sortedByDescending { it.confidence }

            val filtered = allSorted.filter { it.confidence >= threshold }
            val predictions = if (filtered.isEmpty() && allSorted.isNotEmpty()) {
                LocalLogger.w(logTag, "No prediction above threshold=$threshold, " +
                    "returning top prediction as fallback: " +
                    "${allSorted.first().label}=${"%.3f".format(allSorted.first().confidence)}")
                listOf(allSorted.first())
            } else {
                filtered
            }

            if (predictions.isNotEmpty()) {
                LocalLogger.i(logTag, "Top: ${predictions.first().label} " +
                    "(${"%.3f".format(predictions.first().confidence)})")
            }

            Result.Success(predictions)
        } catch (e: Exception) {
            LocalLogger.e(logTag, "Inference failed", e)
            Result.Error(e, "Inference failed: ${e.message}")
        }
    }

    /**
     * Tutup interpreter dan bebaskan resource native.
     * Panggil saat ViewModel/Fragment di-destroy.
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        runtime = null
        LocalLogger.d(logTag, "Interpreter closed")
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRIVATE — Inisialisasi
    // ══════════════════════════════════════════════════════════════════════

    private fun ensureLabels(): List<String> {
        if (labels.isNotEmpty()) return labels
        labels = try {
            val path = resolveLabelPath()
            val loaded = loadLabels(context, path)
            if (loaded.isNotEmpty()) loaded else fallbackLabels
        } catch (e: Exception) {
            LocalLogger.w(logTag, "Cannot read labels from assets, using fallback", e)
            fallbackLabels
        }
        if (labels.size != expectedNumClasses) {
            LocalLogger.w(logTag,
                "Labels count ${labels.size} != expected $expectedNumClasses. " +
                    "Pastikan urutan sesuai label_map.json notebook v3.")
        }
        return labels
    }

    private fun ensureInterpreter(): Pair<Interpreter, ModelRuntime> {
        val existing = interpreter
        val existingRt = runtime
        if (existing != null && existingRt != null) {
            return existing to existingRt
        }

        ensureModelVersion()

        val modelBuffer = loadModelFile(context, modelFileName)
        val options = Interpreter.Options().apply { setNumThreads(4) }
        val interp = Interpreter(modelBuffer, options)
        val rt = buildRuntime(interp)

        LocalLogger.i(logTag,
            "Model loaded: $modelFileName | " +
                "input=${rt.inputWidth}x${rt.inputHeight}x${rt.inputChannels} | " +
                "output=${rt.outputClasses} classes | version=${modelVersion ?: "unknown"}")

        interpreter = interp
        runtime = rt
        return interp to rt
    }

    /**
     * Coba baca versi model dari:
     *  1. model_version.txt  (file kustom opsional)
     *  2. metadata_v3.json   (file yang di-export notebook v3 Section 11)
     */
    private fun ensureModelVersion() {
        if (modelVersion != null) return
        modelVersion = try {
            context.assets.open("model_version.txt").bufferedReader()
                .use { it.readText() }.trim().ifBlank { null }
        } catch (_: Exception) { null }

        if (modelVersion == null) {
            modelVersion = try {
                val json = context.assets.open("metadata_v3.json").bufferedReader()
                    .use { it.readText() }
                Regex(""""model"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
            } catch (_: Exception) { null }
        }

        if (modelVersion == null) {
            LocalLogger.w(logTag, "model_version.txt dan metadata_v3.json tidak ditemukan di assets")
        }
    }

    private fun buildRuntime(interpreter: Interpreter): ModelRuntime {
        val inputShape = interpreter.getInputTensor(0).shape()
        // Shape: [1, H, W, C] — sesuai input_shape=(512,512,3) di notebook v3
        val inputHeight   = if (inputShape.size == 4) inputShape[1] else defaultInputSize
        val inputWidth    = if (inputShape.size == 4) inputShape[2] else defaultInputSize
        val inputChannels = if (inputShape.size == 4) inputShape[3] else 3

        val outputShape  = interpreter.getOutputTensor(0).shape()
        // Shape: [1, NUM_CLASSES] = [1, 5]
        val outputClasses = if (outputShape.size == 2) outputShape[1] else 0

        return ModelRuntime(
            inputWidth    = inputWidth,
            inputHeight   = inputHeight,
            inputChannels = inputChannels,
            outputClasses = outputClasses,
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRIVATE — Inference
    // ══════════════════════════════════════════════════════════════════════

    private fun runInference(
        interpreter: Interpreter,
        bitmap: Bitmap,
        runtime: ModelRuntime
    ): FloatArray {
        val resized = Bitmap.createScaledBitmap(
            bitmap, runtime.inputWidth, runtime.inputHeight, true
        )
        val inputBuffer = convertBitmapToByteBuffer(resized, runtime)
        val outputArray = Array(1) { FloatArray(runtime.outputClasses) }
        interpreter.run(inputBuffer, outputArray)
        return outputArray[0]
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRIVATE — Preprocessing
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Konversi bitmap ke ByteBuffer (FLOAT32).
     *
     * Identik dengan tf.keras.applications.mobilenet_v2.preprocess_input
     * yang dipakai di preprocess_image() & preprocess_tta() notebook v3:
     *   v = (pixel / 127.5f) − 1.0f   →  range [−1.0, 1.0]
     *
     * pixel=0   → −1.000f
     * pixel=127 → −0.004f
     * pixel=128 →  0.004f
     * pixel=255 →  1.000f
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap, runtime: ModelRuntime): ByteBuffer {
        val bufferSize = runtime.inputWidth * runtime.inputHeight * runtime.inputChannels * 4
        val buffer = ByteBuffer.allocateDirect(bufferSize).apply {
            order(ByteOrder.nativeOrder())
        }

        val pixels = IntArray(runtime.inputWidth * runtime.inputHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (px in pixels) {
            val r = (px shr 16) and 0xFF
            val g = (px shr 8)  and 0xFF
            val b =  px         and 0xFF

            val rN = (r / 127.5f) - 1.0f
            val gN = (g / 127.5f) - 1.0f
            val bN = (b / 127.5f) - 1.0f
            val gray = 0.2989f * rN + 0.5870f * gN + 0.1140f * bN

            for (c in 0 until runtime.inputChannels) {
                buffer.putFloat(when {
                    runtime.inputChannels == 1 -> gray
                    c == 0                     -> rN
                    c == 1                     -> gN
                    c == 2                     -> bN
                    c == 3                     -> 1.0f  // alpha — tidak dipakai model ini
                    else                       -> 0.0f
                })
            }
        }

        buffer.rewind()
        return buffer
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRIVATE — Utility
    // ══════════════════════════════════════════════════════════════════════

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        return try {
            val fd: AssetFileDescriptor = context.assets.openFd(modelPath)
            FileInputStream(fd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
            )
        } catch (e: Exception) {
            val bytes = context.assets.open(modelPath).use { it.readBytes() }
            ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                rewind()
            }
        }
    }

    private fun loadLabels(context: Context, path: String): List<String> {
        val list = mutableListOf<String>()
        BufferedReader(InputStreamReader(context.assets.open(path))).use { reader ->
            reader.lineSequence().filter { it.isNotBlank() }.forEach { list.add(it.trim()) }
        }
        return list
    }

    private fun resolveLabelPath(): String {
        val assets = context.assets.list("")?.toList().orEmpty()
        return assets.firstOrNull { it == labelPath }
            ?: assets.firstOrNull { it.endsWith(".txt", true) && "label" in it.lowercase() }
            ?: labelPath
    }
}


private data class ModelRuntime(
    val inputWidth: Int,
    val inputHeight: Int,
    val inputChannels: Int,
    val outputClasses: Int,
)