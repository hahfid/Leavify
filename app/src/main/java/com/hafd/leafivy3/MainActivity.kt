package com.hafd.leafivy3

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hafd.leafivy3.ui.*
import com.hafd.leafivy3.ui.theme.ThemePreferences
import com.hafd.leafivy3.ui.theme.leafivyTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: LeafivyViewModel by viewModels()
    private var pendingCameraNavigation: (() -> Unit)? = null

    private val cameraPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pendingCameraNavigation?.invoke()
                pendingCameraNavigation = null
            } else {
                Toast.makeText(this, getString(R.string.error_camera_permission), Toast.LENGTH_SHORT).show()
                pendingCameraNavigation = null
            }
        }

    override fun attachBaseContext(newBase: Context) {
        // Apply saved language preference to the base context
        val prefs = newBase.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val langName = prefs.getString("language", null)
        val langCode = when (langName) {
            "ENGLISH" -> "en"
            else -> "id" // Default to Indonesian
        }
        val locale = Locale(langCode)
        val config = newBase.resources.configuration.apply {
            setLocale(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ThemePreferences.init(applicationContext)

        setContent {
            // Observe language changes and recreate activity when language switches
            val themePrefs by ThemePreferences.flow.collectAsState()
            val currentLang = themePrefs.language
            val appliedLang = remember { currentLang }

            LaunchedEffect(currentLang) {
                // Only recreate when the user actively changes the language,
                // not on initial composition (which would cause an infinite loop).
                if (currentLang != appliedLang) {
                    recreate()
                }
            }

            leafivyTheme {

                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsState()

                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {

                    composable("splash") {
                        SplashScreen(
                            onDone = {
                                navController.popBackStack()
                                navController.navigate("home")
                            }
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            onTakePhoto = {
                                ensureCameraPermission {
                                    navController.navigate("camera")
                                }
                            },
                            onImageSelected = { bitmap ->
                                viewModel.setPreviewImage(bitmap)
                                navController.navigate("preview")
                            },
                            onOpenAbout = {
                                navController.navigate("about_privacy")
                            },
                            onOpenSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }

                    composable("camera") {
                        CameraScreen(
                            onImageCaptured = { bitmap ->
                                viewModel.setPreviewImage(bitmap)
                                navController.popBackStack()
                                navController.navigate("preview")
                            },
                            onError = { exception ->
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.error_capture_failed, exception.message),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onClose = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("preview") {
                        PreviewScreen(
                            uiState = uiState,
                            onBack = {
                                viewModel.reset()
                                navController.popBackStack()
                            },
                            onProcess = {
                                viewModel.processImage()
                                navController.navigate("result")
                            }
                        )
                    }

                    composable("result") {
                        ResultScreen(
                            uiState = uiState,
                            onBack = {
                                viewModel.reset()
                                navController.popBackStack("home", inclusive = false)
                            },
                            onOpenCareGuide = {
                                navController.navigate("care_guide")
                            },
                            onOpenDiagnostics = {
                                navController.navigate("diagnostics")
                            }
                        )
                    }

                    composable("care_guide") {
                        CareGuideScreen(
                            uiState = uiState,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("diagnostics") {
                        DiagnosticsScreen(
                            uiState = uiState,
                            onBack = { navController.popBackStack() }
                        )
                    }


                    composable("about_privacy") {
                        AboutPrivacyScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun ensureCameraPermission(onGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onGranted()
            return
        }

        pendingCameraNavigation = onGranted
        cameraPermissionRequest.launch(Manifest.permission.CAMERA)
    }
}
