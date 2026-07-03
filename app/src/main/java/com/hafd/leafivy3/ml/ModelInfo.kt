package com.hafd.leafivy3.ml

import org.tensorflow.lite.DataType

data class ModelInfo(
    val modelPath: String,
    val inputWidth: Int,
    val inputHeight: Int,
    val inputChannels: Int,
    val inputType: DataType,
    val outputClasses: Int,
    val labelCount: Int
)
