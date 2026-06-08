package com.g2b.bidapp.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.g2b.bidapp.ui.theme.NavyBlue

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 설정 화면에서 허용 후 돌아왔을 때 설치 재시도
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SplashUiState.NavigateToLogin -> onNavigateToLogin()
            is SplashUiState.NavigateToMain -> onNavigateToMain()
            is SplashUiState.NavigateToDetail -> onNavigateToDetail(state.bidNtceNo)
            else -> Unit
        }
    }

    val showSpinner = when (uiState) {
        is SplashUiState.ForceUpdate,
        is SplashUiState.Downloading,
        is SplashUiState.RecommendUpdate -> false
        else -> true
    }
    SplashBackground(showSpinner = showSpinner)

    when (val state = uiState) {
        is SplashUiState.Loading,
        is SplashUiState.UpToDate,
        is SplashUiState.Error,
        is SplashUiState.NavigateToLogin,
        is SplashUiState.NavigateToMain,
        is SplashUiState.NavigateToDetail -> Unit

        is SplashUiState.ForceUpdate ->
            ForceUpdateDialog(
                releaseNotes = state.releaseNotes,
                onConfirm = { viewModel.onForceUpdateConfirmed(state.downloadUrl) }
            )

        is SplashUiState.Downloading ->
            DownloadProgressDialog(progress = state.progress)

        is SplashUiState.RecommendUpdate ->
            RecommendUpdateDialog(
                releaseNotes = state.releaseNotes,
                onConfirm = { viewModel.onRecommendUpdateConfirmed(state.downloadUrl) },
                onDismiss = { viewModel.onRecommendUpdateDismissed() },
            )
    }

}

@Composable
private fun SplashBackground(showSpinner: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlue),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Image(
                painter = painterResource(id = com.g2b.bidapp.R.drawable.ic_logo),
                contentDescription = "G2B 앱 로고",
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = "나라장터 모니터링",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
            )
            if (showSpinner) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

@Composable
private fun ForceUpdateDialog(
    releaseNotes: String,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "업데이트 필요",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "원활한 서비스 이용을 위해 최신 버전으로 업데이트해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (releaseNotes.isNotBlank()) {
                    Text(
                        text = releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyBlue,
                    ),
                ) {
                    Text("지금 업데이트")
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressDialog(progress: Float) {
    val isIndeterminate = progress < 0f

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "업데이트 다운로드 중...",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (isIndeterminate) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = NavyBlue,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = NavyBlue,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendUpdateDialog(
    releaseNotes: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "새 버전이 있습니다",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("최신 버전으로 업데이트하면 새로운 기능과 개선된 성능을 경험할 수 있습니다.")
                if (releaseNotes.isNotBlank()) {
                    Text(
                        text = releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
            ) {
                Text("지금 업데이트")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("나중에")
            }
        },
    )
}
