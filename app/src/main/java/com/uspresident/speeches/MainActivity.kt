package com.uspresident.speeches

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uspresident.speeches.ads.AdsManager
import com.uspresident.speeches.data.SpeechDetail
import com.uspresident.speeches.ui.NetworkRequiredDialog
import com.uspresident.speeches.ui.presidents.PresidentListScreen
import com.uspresident.speeches.ui.speeches.SpeechDetailScreen
import com.uspresident.speeches.ui.speeches.SpeechDetailViewModel
import com.uspresident.speeches.ui.speeches.SpeechListScreen
import com.uspresident.speeches.ui.theme.PresidentialSpeechesTheme
import com.uspresident.speeches.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var adsManager: AdsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val app = application as SpeechesApplication
        val repository = app.speechRepository

        adsManager = AdsManager(this)
        adsManager.initialize()

        setContent {
            PresidentialSpeechesTheme {
                var isOnline by remember { mutableStateOf(NetworkUtils.isConnected(this)) }

                LaunchedEffect(Unit) {
                    isOnline = NetworkUtils.isConnected(this@MainActivity)
                }

                if (!isOnline) {
                    NetworkRequiredDialog(onConfirmExit = { finish() })
                } else {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "presidents",
                    ) {
                        composable("presidents") {
                            PresidentListScreen(
                                presidents = repository.getPresidents(),
                                onPresidentClick = { president ->
                                    navController.navigate("speeches/${president.id}")
                                },
                            )
                        }

                        composable(
                            route = "speeches/{presidentId}",
                            arguments = listOf(
                                navArgument("presidentId") { type = NavType.StringType },
                            ),
                        ) { entry ->
                            val presidentId = entry.arguments?.getString("presidentId").orEmpty()
                            val president = repository.getPresident(presidentId)
                            val speeches = repository.getSpeeches(presidentId)

                            SpeechListScreen(
                                presidentName = president?.name ?: "Speeches",
                                speeches = speeches,
                                onBack = { navController.popBackStack() },
                                onSpeechClick = { speech ->
                                    navController.navigate("speech/${speech.id}")
                                    adsManager.showInterstitial()
                                },
                            )
                        }

                        composable(
                            route = "speech/{speechId}",
                            arguments = listOf(
                                navArgument("speechId") { type = NavType.StringType },
                            ),
                        ) { entry ->
                            val speechId = entry.arguments?.getString("speechId").orEmpty()
                            var speechDetail by remember(speechId) { mutableStateOf<SpeechDetail?>(null) }
                            var loadError by remember(speechId) { mutableStateOf<String?>(null) }
                            var isLoading by remember(speechId) { mutableStateOf(true) }

                            LaunchedEffect(speechId) {
                                isLoading = true
                                loadError = null
                                if (!NetworkUtils.isConnected(this@MainActivity)) {
                                    loadError = getString(R.string.network_required_message)
                                    speechDetail = null
                                    isLoading = false
                                    return@LaunchedEffect
                                }
                                speechDetail = withContext(Dispatchers.IO) {
                                    repository.getSpeechDetail(speechId)
                                }
                                if (speechDetail == null) {
                                    loadError = repository.getSpeechDetailError(speechId)
                                        ?: getString(R.string.speech_download_failed)
                                }
                                isLoading = false
                            }

                            val detailViewModel: SpeechDetailViewModel = viewModel(
                                factory = object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return SpeechDetailViewModel(
                                            application = application,
                                            onWatchRewardedAd = { onRewardEarned, onFinished ->
                                                adsManager.showForReward(
                                                    onRewardEarned = onRewardEarned,
                                                    onFinished = onFinished,
                                                )
                                            },
                                        ) as T
                                    }
                                },
                            )

                            SpeechDetailScreen(
                                speech = speechDetail,
                                isLoading = isLoading,
                                loadError = loadError,
                                viewModel = detailViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
