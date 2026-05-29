package com.g2b.bidapp.ui.login

import android.app.Activity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.g2b.bidapp.R
import com.g2b.bidapp.ui.theme.BorderGray
import com.g2b.bidapp.ui.theme.ButtonTextDark
import com.g2b.bidapp.ui.theme.G2bBidAppTheme
import com.g2b.bidapp.ui.theme.KakaoYellow
import com.g2b.bidapp.ui.theme.NavyBlue
import com.g2b.bidapp.ui.theme.TextGray

@Composable
fun LoginScreen(
    onNavigateToBidList: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> onNavigateToBidList()
            is LoginUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetError()
            }

            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White,
    ) { innerPadding ->
        LoginContent(
            isLoading = uiState is LoginUiState.Loading,
            onGoogleSignIn = { viewModel.signInWithGoogle(context as Activity) },
            onKakaoSignIn = { viewModel.signInWithKakao() },
            onGuestMode = { viewModel.continueAsGuest() },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun LoginContent(
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit,
    onKakaoSignIn: () -> Unit,
    onGuestMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = "G2B 로고",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "나라장터 입찰공고 모니터링",
                style = MaterialTheme.typography.titleLarge,
                color = NavyBlue,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "실시간으로 입찰공고를 확인하고\n관심공고를 관리하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
        }

        // 로그인 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GoogleSignInButton(
                onClick = onGoogleSignIn,
                enabled = !isLoading,
                isLoading = isLoading,
            )

            KakaoSignInButton(
                onClick = onKakaoSignIn,
                enabled = !isLoading,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGray)
                Text(
                    text = "  또는  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGray)
            }

            TextButton(
                onClick = onGuestMode,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "Guest Mode로 둘러보기",
                    color = TextGray,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (enabled) BorderGray else BorderGray.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = ButtonTextDark,
            disabledContainerColor = Color.White,
            disabledContentColor = ButtonTextDark.copy(alpha = 0.4f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = NavyBlue,
                )
            }

            androidx.compose.animation.AnimatedVisibility(visible = !isLoading, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "G",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4),
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google로 로그인",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = ButtonTextDark,
                    )
                }
            }
        }
    }
}

@Composable
private fun KakaoSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = KakaoYellow,
            contentColor = ButtonTextDark,
            disabledContainerColor = KakaoYellow.copy(alpha = 0.4f),
            disabledContentColor = ButtonTextDark.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "K",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ButtonTextDark,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "카카오로 로그인",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    G2bBidAppTheme {
        LoginContent(
            isLoading = false,
            onGoogleSignIn = {},
            onKakaoSignIn = {},
            onGuestMode = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenLoadingPreview() {
    G2bBidAppTheme {
        LoginContent(
            isLoading = true,
            onGoogleSignIn = {},
            onKakaoSignIn = {},
            onGuestMode = {}
        )
    }
}
