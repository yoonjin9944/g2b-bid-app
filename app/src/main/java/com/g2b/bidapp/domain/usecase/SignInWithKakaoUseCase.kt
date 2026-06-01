package com.g2b.bidapp.domain.usecase

import com.g2b.bidapp.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithKakaoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() : Result<Unit> = authRepository.signInWithKakao()
}