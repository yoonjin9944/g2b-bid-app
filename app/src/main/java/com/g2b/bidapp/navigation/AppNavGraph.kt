package com.g2b.bidapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.g2b.bidapp.domain.model.SearchParams
import com.g2b.bidapp.ui.bid.list.BidListScreen
import com.g2b.bidapp.ui.bid.search.SearchScreen
import com.g2b.bidapp.ui.login.LoginScreen
import com.g2b.bidapp.ui.splash.SplashScreen

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier,
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.BidList.createRoute()) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToBidList = {
                    navController.navigate(Screen.BidList.createRoute()) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "${Screen.BidList.route}?${Screen.BidList.ARG_CATEGORY}={${Screen.BidList.ARG_CATEGORY}}",
            arguments = listOf(
                navArgument(Screen.BidList.ARG_CATEGORY) {
                    type = NavType.StringType
                    defaultValue = Screen.BidList.DEFAULT_CATEGORY
                }
            )
        ) { backStackEntry ->
            // SearchScreen에서 popBackStack()으로 돌아올 때 SearchParams를 savedStateHandle로 전달받음
            val searchParamsFlow = backStackEntry.savedStateHandle
                .getStateFlow<SearchParams?>("searchParams", null)
            val incomingSearchParams by searchParamsFlow.collectAsStateWithLifecycle()

            BidListScreen(
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                incomingSearchParams = incomingSearchParams,
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onSearch = { params ->
                    // BidListScreen의 savedStateHandle에 SearchParams 전달
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("searchParams", params)
                    navController.popBackStack()
                },
            )
        }

        // BidDetail route — Phase 5에서는 BidListScreen 내부 ModalBottomSheet로 처리됨.
        // WatchlistScreen(Phase 7)에서도 동일 패턴 사용. route는 향후 딥링크 확장을 위해 유지.
        composable(
            route = Screen.BidDetail.route,
            arguments = listOf(
                navArgument(Screen.BidDetail.ARG_BID_NTCE_NO) {
                    type = NavType.StringType
                }
            )
        ) {
            // 현재 미사용. ModalBottomSheet는 각 화면 내부에서 직접 관리.
        }

        composable(Screen.Watchlist.route) { }
        composable(Screen.BidResult.route) { }
        composable(Screen.Notifications.route) { }
        composable(Screen.Settings.route) { }
    }
}
