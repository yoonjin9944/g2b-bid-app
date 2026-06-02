package com.g2b.bidapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.g2b.bidapp.navigation.AppNavGraph
import com.g2b.bidapp.ui.theme.G2bBidAppTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var auth: Auth

    @Inject
    lateinit var authRedirectFlow: MutableSharedFlow<String>

    private val TAG = "MainActivity"

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 사용자 결정은 시스템이 저장 — 별도 처리 불필요 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 최초 실행 시 딥링크가 포함되어 들어온 경우 처리
        intent?.data?.let { uri ->
            handleAuthRedirect(uri)
        }

        setContent {
            G2bBidAppTheme {
                AppNavGraph()
            }
        }
    }

    // singleTop 모드에서 외부 브라우저(카카오 인증)로부터 복귀할 때 호출됨
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // [수정] 혼선을 주던 과거 런타임 주석 코드를 완전히 제거하여 가독성을 높였습니다.

        intent.data?.let { uri ->
            handleAuthRedirect(uri)
        }
    }

    private fun handleAuthRedirect(uri: Uri) {
        Log.d(TAG, "수신된 딥링크 URI: $uri")
        if (uri.scheme == "com.g2b.bidapp") {
            // [수정] tryEmit의 데이터 유실 가능성을 차단하기 위해 lifecycleScope를 활용한 동기적 emit() 구조로 변경합니다.
            // Repository 레이어에서 .first()로 대기할 때까지 스트림의 안정적 전달을 보장합니다.
            lifecycleScope.launch {
                authRedirectFlow.emit(uri.toString())
                Log.d(TAG, "authRedirectFlow에 딥링크 URL 전달 완료")
            }
        }
    }
}