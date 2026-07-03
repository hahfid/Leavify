package com.hafd.leafivy3.ui

import android.graphics.Bitmap
import android.view.ViewGroup
import android.view.MotionEvent
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hafd.leafivy3.R
import com.hafd.leafivy3.utils.BitmapUtils
import com.hafd.leafivy3.utils.LocalLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun CameraScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var shutterFlash by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val flashAlpha by animateFloatAsState(
        targetValue = if (shutterFlash) 0.7f else 0f,
        animationSpec = tween(140),
        label = "shutter_flash"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(previewView.display.rotation)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        cameraControl = camera.cameraControl
                    } catch (exc: Exception) {
                        LocalLogger.e("CameraScreen", "Use case binding failed", exc)
                    }
                }, executor)

                previewView.setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val factory = previewView.meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point)
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build()
                        cameraControl?.startFocusAndMetering(action)
                        view.performClick()
                    }
                    true
                }

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (showGrid) {
            GridOverlay(modifier = Modifier.fillMaxSize())
        }

        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(flashAlpha)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }

        // Top controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = {
                            torchEnabled = !torchEnabled
                            cameraControl?.enableTorch(torchEnabled)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = stringResource(R.string.cd_flash_toggle),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showGrid = !showGrid },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                            contentDescription = stringResource(R.string.cd_grid_toggle),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape
                            )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f))
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = stringResource(R.string.cd_capture_button),
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.0f)
                )
            }

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f))
                    .clickable {
                        val imgCapture = imageCapture ?: return@clickable
                        val photoParent = context.externalCacheDir ?: context.cacheDir
                        val photoFile = File(photoParent, "leafivy_${System.currentTimeMillis()}.jpg")

                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        shutterFlash = true
                        scope.launch {
                            delay(160)
                            shutterFlash = false
                        }

                        imgCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val bitmap = BitmapUtils.decodeSampledBitmapFromFile(
                                        photoFile.absolutePath,
                                        maxDimension = 1024
                                    )
                                    if (bitmap != null) {
                                        val optimized = BitmapUtils.optimizeBitmap(
                                            bitmap,
                                            imagePath = photoFile.absolutePath,
                                            maxDimension = 1024
                                        )
                                        onImageCaptured(optimized)
                                    } else {
                                        LocalLogger.w("CameraScreen", "Failed to decode captured image")
                                    }
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    onError(exc)
                                }
                            }
                        )
                    }
                    .semantics {
                        contentDescription = context.getString(R.string.cd_capture_button)
                    }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = stringResource(R.string.copyright_line),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun GridOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val thirdW = size.width / 3f
            val thirdH = size.height / 3f
            val color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f)
            val stroke = 1.dp.toPx()

            drawLine(color, androidx.compose.ui.geometry.Offset(thirdW, 0f), androidx.compose.ui.geometry.Offset(thirdW, size.height), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(thirdW * 2f, 0f), androidx.compose.ui.geometry.Offset(thirdW * 2f, size.height), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(0f, thirdH), androidx.compose.ui.geometry.Offset(size.width, thirdH), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(0f, thirdH * 2f), androidx.compose.ui.geometry.Offset(size.width, thirdH * 2f), stroke)
        }
    }
}
