package com.g2b.bidapp.ui.theme

import androidx.compose.ui.graphics.Color

/** 앱 아이콘 배경, 타이틀 텍스트, Primary 색상 */
val NavyBlue        = Color(0xFF001E40)

/** 부제목, 링크 텍스트 */
val TextGray        = Color(0xFF43474F)

/** Google 버튼 텍스트 */
val ButtonTextDark  = Color(0xFF191C1D)

/** Google 버튼 테두리 */
val BorderGray      = Color(0xFFC3C6D1)

/** 카카오 버튼 배경 */
val KakaoYellow     = Color(0xFFFEE500)

// ── 공고 상태 배지 색상 ───────────────────────────────────────────────────────

/** 변경 상태 (주황) */
val StatusChanged   = Color(0xFFFF6D00)

/** 취소 상태 (빨강) */
val StatusCancelled = Color(0xFFBA1A1A)

/** 재공고 상태 (파랑) */
val StatusReopened  = Color(0xFF0057CC)

/** 개찰 상태 (회색) */
val StatusOpened    = Color(0xFF6E7680)

// ── Material3 Light Scheme 색상 ──────────────────────────────────────────────

val PrimaryLight            = NavyBlue
val OnPrimaryLight          = Color(0xFFFFFFFF)
val PrimaryContainerLight   = Color(0xFFD4E4FF)
val OnPrimaryContainerLight = Color(0xFF001D35)

val SecondaryLight            = Color(0xFF535F70)
val OnSecondaryLight          = Color(0xFFFFFFFF)
val SecondaryContainerLight   = Color(0xFFD6E4F7)
val OnSecondaryContainerLight = Color(0xFF0F1D2A)

val TertiaryLight            = Color(0xFF695779)
val OnTertiaryLight          = Color(0xFFFFFFFF)
val TertiaryContainerLight   = Color(0xFFEFDBFF)
val OnTertiaryContainerLight = Color(0xFF241432)

val ErrorLight            = StatusCancelled
val OnErrorLight          = Color(0xFFFFFFFF)
val ErrorContainerLight   = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight   = Color(0xFFF8F9FF)
val OnBackgroundLight = Color(0xFF191C20)
val SurfaceLight      = Color(0xFFF8F9FF)
val OnSurfaceLight    = Color(0xFF191C20)

val SurfaceVariantLight   = Color(0xFFDFE2EB)
val OnSurfaceVariantLight = Color(0xFF43474E)
val OutlineLight          = BorderGray

// ── Material3 Dark Scheme 색상 ──────────────────────────────────────────────

val PrimaryDark            = Color(0xFFA5C8FF)
val OnPrimaryDark          = Color(0xFF003257)
val PrimaryContainerDark   = Color(0xFF00497D)
val OnPrimaryContainerDark = Color(0xFFD4E4FF)

val SecondaryDark            = Color(0xFFBAC8DB)
val OnSecondaryDark          = Color(0xFF243140)
val SecondaryContainerDark   = Color(0xFF3B4857)
val OnSecondaryContainerDark = Color(0xFFD6E4F7)

val TertiaryDark            = Color(0xFFD3BEE4)
val OnTertiaryDark          = Color(0xFF392948)
val TertiaryContainerDark   = Color(0xFF503F60)
val OnTertiaryContainerDark = Color(0xFFEFDBFF)

val ErrorDark            = Color(0xFFFFB4AB)
val OnErrorDark          = Color(0xFF690005)
val ErrorContainerDark   = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark   = Color(0xFF111318)
val OnBackgroundDark = Color(0xFFE2E2E9)
val SurfaceDark      = Color(0xFF111318)
val OnSurfaceDark    = Color(0xFFE2E2E9)

val SurfaceVariantDark   = Color(0xFF43474E)
val OnSurfaceVariantDark = Color(0xFFC3C7CF)
val OutlineDark          = Color(0xFF8D9199)