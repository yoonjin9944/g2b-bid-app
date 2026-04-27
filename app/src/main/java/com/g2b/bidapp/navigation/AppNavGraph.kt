package com.g2b.bidapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            PlaceholderScreen("SplashScreen")
        }

        composable(Screen.Login.route) {
            PlaceholderScreen("LoginScreen")
        }

        composable(
            route = Screen.BidList.route,
            arguments = listOf(
                navArgument(Screen.BidList.ARG_CATEGORY) {
                    type = NavType.StringType
                    defaultValue = Screen.BidList.DEFAULT_CATEGORY
                }
            )
        ) { backStackEntry ->
            val category =
                backStackEntry.arguments?.getString(Screen.BidList.ARG_CATEGORY) ?: Screen.BidList.DEFAULT_CATEGORY
            PlaceholderScreen("BidListScreen\ncategory=$category")
        }

        composable(Screen.Search.route) {
            PlaceholderScreen("SearchScreen")
        }

        composable(
            route = Screen.BidDetail.route,
            arguments = listOf(
                navArgument(Screen.BidDetail.ARG_BID_NTCE_NO) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val bidNtceNo = backStackEntry.arguments?.getString(Screen.BidDetail.ARG_BID_NTCE_NO).orEmpty()
            PlaceholderScreen("BidDetailScreen\nbidNtceNo=$bidNtceNo")
        }

        composable(Screen.Watchlist.route) {
            PlaceholderScreen("WatchlistScreen")
        }

        composable(Screen.BidResult.route) {
            PlaceholderScreen("BidResultScreen")
        }

        composable(Screen.Notifications.route) {
            PlaceholderScreen("NotificationListScreen")
        }

        composable(Screen.Settings.route) {
            PlaceholderScreen("SettingsScreen")
        }
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label)
    }
}