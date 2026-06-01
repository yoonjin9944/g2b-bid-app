package com.g2b.bidapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.g2b.bidapp.ui.bid.list.BidListScreen
import com.g2b.bidapp.ui.bid.result.BidResultListScreen
import com.g2b.bidapp.ui.bid.search.SearchScreen
import com.g2b.bidapp.ui.bid.search.SearchSharedViewModel
import com.g2b.bidapp.ui.bid.watchlist.WatchlistScreen
import com.g2b.bidapp.ui.login.LoginScreen
import com.g2b.bidapp.ui.notification.NotificationListScreen
import com.g2b.bidapp.ui.settings.SettingsScreen
import com.g2b.bidapp.ui.splash.SplashScreen
import com.g2b.bidapp.ui.theme.NavyBlue

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.BidList.createRoute(), "입찰공고", Icons.Outlined.Description),
    BottomNavItem(Screen.Watchlist.route, "관심공고", Icons.Outlined.Favorite),
//    BottomNavItem(Screen.BidResult.route, "낙찰결과", Icons.Outlined.EmojiEvents),
    BottomNavItem(Screen.Notifications.route, "알림", Icons.Outlined.Notifications),
    BottomNavItem(Screen.Settings.route, "설정", Icons.Outlined.Settings),
)

// BottomNavigation을 표시할 route 목록 (route 템플릿 기준)
private val bottomNavRoutes = setOf(
    "${Screen.BidList.route}?${Screen.BidList.ARG_CATEGORY}={${Screen.BidList.ARG_CATEGORY}}",
    Screen.Watchlist.route,
    Screen.BidResult.route,
    Screen.Notifications.route,
    Screen.Settings.route,
)

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val searchSharedViewModel: SearchSharedViewModel = hiltViewModel()
    val incomingSearchParams by searchSharedViewModel.pendingParams.collectAsStateWithLifecycle()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomNav = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                AppBottomNavigationBar(
                    currentRoute = currentRoute,
                    onItemSelected = { item ->
                        navController.navigate(item.route) {
                            // 탭 전환 시 스택 최소화: startDestination까지 popUpTo
                            popUpTo(Screen.BidList.createRoute()) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
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
            ) {
                BidListScreen(
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    incomingSearchParams = incomingSearchParams,
                    onSearchConsumed = { searchSharedViewModel.consume() },
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSearch = { params ->
                        searchSharedViewModel.submit(params)
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

            // Phase 7: WatchlistScreen 추가
            composable(Screen.Watchlist.route) {
                WatchlistScreen()
            }

            composable(Screen.BidResult.route) {
                BidResultListScreen()
            }

            composable(Screen.Notifications.route) {
                NotificationListScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onLoggedOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AppBottomNavigationBar(
    currentRoute: String?,
    onItemSelected: (BottomNavItem) -> Unit,
) {
    NavigationBar(
        containerColor = Color(0xFFF8FAFC),
        tonalElevation = 0.dp,
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = when (item.route) {
                // BidList는 route 템플릿으로 비교
                Screen.BidList.createRoute() ->
                    currentRoute?.startsWith(Screen.BidList.route) == true

                else -> currentRoute == item.route
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(item) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NavyBlue,
                    selectedTextColor = NavyBlue,
                    indicatorColor = Color(0xFFDCE9FF),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8),
                ),
            )
        }
    }
}
