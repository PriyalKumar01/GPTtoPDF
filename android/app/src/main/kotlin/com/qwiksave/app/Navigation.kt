package com.qwiksave.app

import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.qwiksave.app.data.UserPreferences
import com.qwiksave.app.ui.components.OnboardingScreen
import com.qwiksave.app.ui.components.SkeletonLoader
import com.qwiksave.app.ui.components.SplashScreen
import com.qwiksave.app.ui.main.MainScreen

enum class AppStage {
    SPLASH,
    ONBOARDING,
    MAIN
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current

    var currentStage by remember { mutableStateOf(AppStage.SPLASH) }

    AnimatedContent(
        targetState = currentStage,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "AppStageTransition",
        modifier = Modifier.fillMaxSize()
    ) { stage ->
        when (stage) {
            AppStage.SPLASH -> {
                SplashScreen(
                    onSplashFinished = {
                        val onboardingDone = UserPreferences.hasCompletedOnboarding(context)
                        currentStage = if (onboardingDone) {
                            AppStage.MAIN
                        } else {
                            AppStage.ONBOARDING
                        }
                    }
                )
            }

            AppStage.ONBOARDING -> {
                OnboardingScreen(
                    onComplete = { userName ->
                        UserPreferences.setUserName(context, userName)
                        UserPreferences.setOnboardingCompleted(context, true)
                        currentStage = AppStage.MAIN
                    }
                )
            }

            AppStage.MAIN -> {
                MainScreen()
            }
        }
    }
}
