package com.hafd.leafivy3.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import com.hafd.leafivy3.utils.LocalLogger
import com.hafd.leafivy3.utils.Result
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max

class LeafGateClassifier(
    private val context: Context,
    private val modelPath: String = "leaf_gate_model.tflite",
    private val labelPath: String = "imagenet_labels.txt",
    private val minPlantScore: Float = 0.25f
) {
    private val logTag = "LeafGateClassifier"
    private var interpreter: Interpreter? = null
    private var isInitialized = false
    private var initializationError: Exception? = null
    private var labels: List<String> = emptyList()
    private var inputWidth = 224
    private var inputHeight = 224
    private var inputChannels = 3
    private var inputDataType: DataType = DataType.FLOAT32
    private var inputBufferSize = 0
    private var outputClasses = 0

    init {
        setup()
    }

    private fun setup() {
        try {
            val options = Interpreter.Options().apply { setNumThreads(4) }
            val modelBuffer = loadModelFile(context, modelPath)
            interpreter = Interpreter(modelBuffer, options)
            labels = loadLabels(context, labelPath)

            val inputTensor = interpreter?.getInputTensor(0)
            val inputShape = inputTensor?.shape()
            if (inputShape != null && inputShape.size == 4) {
                inputHeight = inputShape[1]
                inputWidth = inputShape[2]
                inputChannels = inputShape[3]
                inputDataType = inputTensor.dataType()
            }
            val bytesPerChannel = if (inputDataType == DataType.UINT8) 1 else 4
            inputBufferSize = inputHeight * inputWidth * inputChannels * bytesPerChannel

            val outputTensor = interpreter?.getOutputTensor(0)
            val outputShape = outputTensor?.shape()
            if (outputShape != null && outputShape.size == 2) {
                outputClasses = outputShape[1]
            }
            if (outputClasses == 0) {
                outputClasses = labels.size.coerceAtLeast(1)
            }

            LocalLogger.d(
                logTag,
                "Gate model loaded: $modelPath, labels=${labels.size}, " +
                    "inputShape=${inputShape?.contentToString()}, inputType=$inputDataType, " +
                    "outputShape=${outputShape?.contentToString()}"
            )
            isInitialized = true
            initializationError = null
        } catch (e: Exception) {
            LocalLogger.e(logTag, "Failed to initialize gate model", e)
            isInitialized = false
            initializationError = e
        }
    }

    fun isLeaf(bitmap: Bitmap): Result<Boolean> {
        if (!isInitialized || interpreter == null) {
            setup()
            if (!isInitialized || interpreter == null) {
                return Result.Error(
                    initializationError ?: Exception("Gate model not initialized"),
                    "Failed to load the leaf gate model."
                )
            }
        }

        return try {
            val resized = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
            val outputArray = if (inputDataType == DataType.FLOAT32) {
                val output01 = runInference(resized, LeafPreprocessMode.SCALE_0_1)
                val outputM11 = runInference(resized, LeafPreprocessMode.SCALE_MINUS1_1)

                val score01 = plantScore(output01[0])
                val scoreM11 = plantScore(outputM11[0])
                if (scoreM11 > score01) {
                    LocalLogger.d(logTag, "Preprocess picked: -1..1 (score=$scoreM11)")
                    outputM11
                } else {
                    LocalLogger.d(logTag, "Preprocess picked: 0..1 (score=$score01)")
                    output01
                }
            } else {
                runInference(resized, LeafPreprocessMode.SCALE_0_1)
            }

            val score = plantScore(outputArray[0])
            Result.Success(score >= minPlantScore)
        } catch (e: Exception) {
            LocalLogger.e(logTag, "Gate classification failed", e)
            Result.Error(e, "Failed to analyze the image.")
        }
    }

    private fun runInference(bitmap: Bitmap, mode: LeafPreprocessMode): Array<FloatArray> {
        val inputBuffer = toByteBuffer(bitmap, mode)
        val output = Array(1) { FloatArray(outputClasses.coerceAtLeast(1)) }
        interpreter?.run(inputBuffer, output)
        return output
    }

    private fun toByteBuffer(bitmap: Bitmap, mode: LeafPreprocessMode): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(inputBufferSize)
        buffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until inputHeight) {
            for (j in 0 until inputWidth) {
                val value = intValues[pixel++]
                val r = (value shr 16 and 0xFF)
                val g = (value shr 8 and 0xFF)
                val b = (value and 0xFF)

                if (inputDataType == DataType.UINT8) {
                    buffer.put(r.toByte())
                    buffer.put(g.toByte())
                    buffer.put(b.toByte())
                } else {
                    when (mode) {
                        LeafPreprocessMode.SCALE_0_1 -> {
                            buffer.putFloat(r / 255.0f)
                            buffer.putFloat(g / 255.0f)
                            buffer.putFloat(b / 255.0f)
                        }
                        LeafPreprocessMode.SCALE_MINUS1_1 -> {
                            buffer.putFloat((r - 127.5f) / 127.5f)
                            buffer.putFloat((g - 127.5f) / 127.5f)
                            buffer.putFloat((b - 127.5f) / 127.5f)
                        }
                    }
                }
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun plantScore(probabilities: FloatArray): Float {
        if (labels.isEmpty()) return 0f

        var score = 0f
        val keywordList = listOf(
            "leaf", "plant", "potato", "tree", "flower", "herb", "grass",
            "tomato", "pepper", "cabbage", "broccoli", "cauliflower",
            "lettuce", "corn", "wheat", "barley", "strawberry",
            "orange", "lemon", "banana", "apple", "pear", "grape",
            "pineapple", "pumpkin", "squash", "cucumber", "vegetable",
            "fruit"
        )

        val size = minOf(probabilities.size, labels.size)
        for (i in 0 until size) {
            val label = labels[i].lowercase()
            if (keywordList.any { it in label }) {
                score = max(score, probabilities[i])
            }
        }
        return score
    }

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        return try {
            val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel: FileChannel = inputStream.channel
            val startOffset: Long = fileDescriptor.startOffset
            val declaredLength: Long = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            val bytes = context.assets.open(modelPath).use { it.readBytes() }
            val buffer = ByteBuffer.allocateDirect(bytes.size)
            buffer.order(ByteOrder.nativeOrder())
            buffer.put(bytes)
            buffer.rewind()
            buffer
        }
    }

    private fun loadLabels(context: Context, labelPath: String): List<String> {
        val labels = ArrayList<String>()
        val reader = BufferedReader(InputStreamReader(context.assets.open(labelPath)))
        var line: String? = reader.readLine()
        while (line != null) {
            if (line.isNotEmpty()) labels.add(line)
            line = reader.readLine()
        }
        reader.close()
        return labels
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

private enum class LeafPreprocessMode {
    SCALE_0_1,
    SCALE_MINUS1_1
}
