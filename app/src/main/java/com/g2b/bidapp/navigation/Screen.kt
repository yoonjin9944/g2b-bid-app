package com.g2b.bidapp.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")

    // 탭 Navigation
    data object BidList : Screen("bid/list") {
        const val ARG_CATEGORY = "category"
        const val DEFAULT_CATEGORY = "CNSTWK"

        fun createRoute(category: String = DEFAULT_CATEGORY) = "bid/list?category=$category"
    }

    data object Watchlist : Screen("watchlist")
    data object BidResult : Screen("result")
    data object Notifications : Screen("notifications")
    data object Settings : Screen("settings")

    data object Search : Screen("bid/search")

    data object BidDetail : Screen("bid/detail/{bidNtceNo}") {
        const val ARG_BID_NTCE_NO = "bidNtceNo"

        fun createRoute(bidNtceNo: String) = "bid/detail/$bidNtceNo"
    }
}