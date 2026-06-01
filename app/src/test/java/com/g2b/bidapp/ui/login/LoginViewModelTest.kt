package com.g2b.bidapp.ui.login

import com.g2b.bidapp.domain.model.User
import com.g2b.bidapp.domain.repository.AuthRepository
import com.g2b.bidapp.domain.usecase.SignInWithGoogleUseCase
import com.g2b.bidapp.domain.usecase.UserCancelledSignInException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var signInUseCase: SignInWithGoogleUseCase
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    private val sampleUser = User(id = "uid-123", email = "test@google.com", displayName = "테스터")

    @Before
    fun setUp() {
        signInUseCase = mockk()
        authRepository = mockk(relaxed = true)
        viewModel = LoginViewModel(
            signInWithGoogleUseCase = signInUseCase,
            authRepository = authRepository,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun `Google 로그인 성공 시 Loading → Success 순서로 전이된다`() = runTest(testDispatcher) {
        // Given
        val states = mutableListOf<LoginUiState>()
        val job = launch(testDispatcher) { viewModel.uiState.collect { states.add(it) } }

        coEvery {
            signInUseCase.getGoogleIdToken(any(), any())
        } returns Result.success("fake-id-token")

        coEvery {
            signInUseCase.signIn("fake-id-token")
        } returns Result.success(sampleUser)

        viewModel.signInWithGoogle(mockk(relaxed = true))

        assertTrue(states.any { it is LoginUiState.Loading })
        assertTrue(states.last() is LoginUiState.Success)
        assertEquals(sampleUser, (states.last() as LoginUiState.Success).user)

        job.cancel()
    }

    @Test
    fun `Google 로그인 실패 시 Loading → Error 로 전이된다`() = runTest(testDispatcher) {
        // Given
        val states = mutableListOf<LoginUiState>()
        val job = launch(testDispatcher) { viewModel.uiState.collect { states.add(it) } }

        coEvery {
            signInUseCase.getGoogleIdToken(any(), any())
        } returns Result.success("fake-id-token")

        coEvery {
            signInUseCase.signIn("fake-id-token")
        } returns Result.failure(RuntimeException("Supabase 인증 오류"))

        viewModel.signInWithGoogle(mockk(relaxed = true))

        assertTrue(states.any { it is LoginUiState.Loading })
        val lastState = states.last()
        assertTrue(lastState is LoginUiState.Error)
        assertEquals("Supabase 인증 오류", (lastState as LoginUiState.Error).message)

        job.cancel()
    }

    @Test
    fun `사용자가 로그인 취소 시 Error 없이 Idle로 복귀한다`() = runTest(testDispatcher) {
        val states = mutableListOf<LoginUiState>()
        val job = launch(testDispatcher) { viewModel.uiState.collect { states.add(it) } }

        coEvery {
            signInUseCase.getGoogleIdToken(any(), any())
        } returns Result.failure(UserCancelledSignInException())

        viewModel.signInWithGoogle(mockk(relaxed = true))

        assertTrue(states.none { it is LoginUiState.Error })
        assertTrue(states.last() is LoginUiState.Idle)

        job.cancel()
    }

    @Test
    fun `Guest 모드 진입 시 즉시 Success(Guest) 상태로 전이된다`() = runTest(testDispatcher) {
        val states = mutableListOf<LoginUiState>()
        val job = launch(testDispatcher) { viewModel.uiState.collect { states.add(it) } }

        viewModel.continueAsGuest()

        val lastState = states.last()
        assertTrue(lastState is LoginUiState.Success)
        assertEquals(LoginViewModel.GUEST_USER_ID, (lastState as LoginUiState.Success).user.id)

        job.cancel()
    }

    @Test
    fun `Loading 중 중복 signInWithGoogle 호출은 무시된다`() = runTest(testDispatcher) {
        // Given
        val states = mutableListOf<LoginUiState>()
        val job = launch(testDispatcher) { viewModel.uiState.collect { states.add(it) } }

        coEvery {
            signInUseCase.getGoogleIdToken(any(), any())
        } coAnswers { kotlinx.coroutines.delay(10_000); Result.success("token") }

        viewModel.signInWithGoogle(mockk(relaxed = true))
        viewModel.signInWithGoogle(mockk(relaxed = true))

        assertEquals(1, states.count { it is LoginUiState.Loading })

        job.cancel()
    }
}