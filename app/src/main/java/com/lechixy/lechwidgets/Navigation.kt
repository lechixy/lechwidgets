package com.lechixy.lechwidgets

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.lechixy.lechwidgets.common.Route
import com.lechixy.lechwidgets.page.settings.About
import com.lechixy.lechwidgets.page.Home
import com.lechixy.lechwidgets.page.Settings
import com.lechixy.lechwidgets.page.settings.General
import com.lechixy.lechwidgets.ui.theme.LechWidgetsTheme

@Composable
fun Navigation(activity: Activity, initialFavoriteRoute: String) {
    val navController = rememberNavController()
    val onBackPressed: () -> Unit = { navController.popBackStack() }

    LechWidgetsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController,
                startDestination = initialFavoriteRoute,
                enterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(220, 55),
                        initialOffsetX = { (it * 0.1f).toInt() }
                    ) + fadeIn(
                        animationSpec = tween(
                            220
                        )
                    )
                },
                exitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(220, 55),
                        targetOffsetX = { -(it * 0.1f).toInt() }
                    ) + fadeOut(
                        animationSpec = tween(
                            220
                        )
                    )
                },
                popEnterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(220, 55),
                        initialOffsetX = { -(it * 0.1f).toInt() }
                    ) + fadeIn(
                        animationSpec = tween(
                            220
                        )
                    )
                },
                popExitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(220, 55),
                        targetOffsetX = { (it * 0.1f).toInt() }
                    ) + fadeOut(
                        animationSpec = tween(
                            220
                        )
                    )
                }
            ) {
                composable(Route.HOME) {
                    Home(
                        navigateToSettings = {
                            navController.navigate(Route.SETTINGS)
                        },
                        activity
                    )
                }

                settingsGraph(navController = navController)
            }
        }
    }
}

fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    onBackPressed: () -> Unit = { navController.popBackStack() }
) {
    navigation(startDestination = Route.SETTINGS_PAGE, route = Route.SETTINGS) {
        composable(Route.SETTINGS_PAGE) {
            Settings(
                navController,
                onBackPressed
            )
        }
        composable(Route.SETTINGS_GENERAL) {
            General(
                onBackPressed
            )
        }
        composable(Route.SETTINGS_ABOUT) {
            About(
                onBackPressed
            )
        }
    }
}