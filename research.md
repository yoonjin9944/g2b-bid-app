# G2B 입찰공고 모니터링 앱 — 신규 프로젝트 분석 보고서

> 작성일: 2026-04-22  
> 현재 환경: compileSdk 36 / minSdk 33 / AGP 9.0.0-alpha06 / Kotlin 2.0.21  
> 목적: SI 개발업체 영업팀을 위한 조달청 나라장터 입찰공고 실시간 모니터링 Android 앱

---

## 1. 프로젝트 모듈 구조

### 전략: 단일 모듈 + 계층별 패키지 분리 (Pragmatic Clean Architecture)

멀티모듈(`:data`, `:domain`, `:presentation` 별도 Gradle 모듈)은 빌드 캐시 병렬화에 유리하지만, 초기 팀 구성과 단일 앱 규모를 고려하면 모듈 경계 설정 및 Hilt 멀티모듈 설정 오버헤드가 실익을 넘습니다. **단일 `:app` 모듈 내에서 패키지 경계로 계층을 분리**하는 전략을 채택합니다.

```
프로젝트 루트
├── :app                     ← 유일한 Android 모듈
├── scripts/
│   └── poll-bid-status.js   ← GitHub Actions에서 실행하는 Node.js 폴링 스크립트
├── .github/
│   └── workflows/
│       └── bid-polling.yml  ← GitHub Actions Cron 워크플로우
├── build.gradle.kts         ← 루트 빌드 (플러그인 버전만 선언)
├── gradle/
│   └── libs.versions.toml   ← Version Catalog (라이브러리 버전 중앙 관리)
└── settings.gradle.kts
```

### 계층 간 의존성 방향 (단방향 강제)

```
presentation → domain ← data
```

- `domain` 패키지: Android 프레임워크, Retrofit, Room 등 외부 라이브러리 import 금지. 순수 Kotlin 클래스만 허용.
- `data` 패키지: `domain`의 Repository 인터페이스를 구현.
- `presentation` 패키지: `domain`의 UseCase만 호출. `data` 패키지 직접 참조 금지.
- `di` 패키지: 계층 경계를 넘는 의존성 조립(Hilt Module)만 허용.

---

## 2. 패키지 폴더 구조 전체

패키지 루트: `com.g2b.bidapp`  
(현재 `com.example.g2b_bid_app` → 정식 패키지명으로 변경 필요)

```
com.g2b.bidapp/
│
├── G2bApplication.kt                    # Application 클래스 (@HiltAndroidApp)
│
├── di/                                  # Hilt 의존성 조립
│   ├── NetworkModule.kt                 # Retrofit, OkHttpClient, Interceptor 제공
│   ├── DatabaseModule.kt                # Room AppDatabase 제공
│   ├── RepositoryModule.kt              # Repository 인터페이스 → 구현체 바인딩
│   └── SupabaseModule.kt                # SupabaseClient 제공
│
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   ├── BidPublicInfoApi.kt      # 입찰공고정보서비스 Retrofit 인터페이스
│   │   │   └── ScsbidInfoApi.kt         # 낙찰정보서비스 Retrofit 인터페이스
│   │   ├── dto/
│   │   │   ├── BidNoticeDto.kt          # 공고 목록 응답 DTO
│   │   │   ├── BidResultDto.kt          # 낙찰 결과 응답 DTO
│   │   │   └── ApiResponse.kt           # 공통 응답 래퍼 (response.body.items)
│   │   └── interceptor/
│   │       ├── AuthInterceptor.kt       # ServiceKey + type=json 자동 주입
│   │       └── RetryInterceptor.kt      # HTTP 429 지수 백오프 재시도
│   │
│   ├── local/
│   │   ├── database/
│   │   │   └── AppDatabase.kt           # Room Database 선언
│   │   ├── dao/
│   │   │   ├── WatchedBidDao.kt         # 관심공고 DAO
│   │   │   └── BidStatusHistoryDao.kt   # 상태변경 이력 DAO
│   │   └── entity/
│   │       ├── WatchedBidEntity.kt      # 관심공고 Room Entity
│   │       └── BidStatusHistoryEntity.kt
│   │
│   ├── supabase/
│   │   ├── dto/
│   │   │   ├── SupabaseBidNotice.kt     # bid_notices 테이블 직렬화 모델
│   │   │   └── SupabaseUser.kt          # users 테이블 직렬화 모델
│   │   └── service/
│   │       ├── SupabaseAuthService.kt   # Google ID Token → Supabase 세션
│   │       └── SupabaseRealtimeService.kt # Realtime 채널 구독/해제 관리
│   │
│   ├── paging/
│   │   ├── BidNoticePagingSource.kt     # 공고 목록 PagingSource (공사/용역/물품 공통)
│   │   └── BidResultPagingSource.kt     # 낙찰 결과 PagingSource
│   │
│   ├── repository/
│   │   ├── BidRepositoryImpl.kt         # BidRepository 구현
│   │   ├── WatchlistRepositoryImpl.kt   # WatchlistRepository 구현
│   │   └── AuthRepositoryImpl.kt        # AuthRepository 구현
│   │
│   └── mapper/
│       ├── BidNoticeMapper.kt           # DTO → Domain Model 변환
│       └── WatchedBidMapper.kt          # Room Entity → Domain Model 변환
│
├── domain/
│   ├── model/
│   │   ├── BidNotice.kt                 # 입찰공고 도메인 모델
│   │   ├── BidResult.kt                 # 낙찰 결과 도메인 모델
│   │   ├── WatchedBid.kt                # 관심공고 도메인 모델
│   │   ├── BidCategory.kt               # 업종 Enum: CNSTWK / SERVC / THNG
│   │   ├── BidStatus.kt                 # 공고 상태 Enum: REGISTERED / CHANGED / CANCELLED / REOPENED / OPENED
│   │   └── User.kt                      # 사용자 도메인 모델
│   │
│   ├── repository/
│   │   ├── BidRepository.kt             # 공고 조회 인터페이스
│   │   ├── WatchlistRepository.kt       # 관심공고 관리 인터페이스
│   │   └── AuthRepository.kt            # 인증 인터페이스
│   │
│   └── usecase/
│       ├── bid/
│       │   ├── GetBidNoticeListUseCase.kt    # 공고 목록 Flow<PagingData> 반환
│       │   └── GetBidResultListUseCase.kt    # 낙찰 결과 Flow<PagingData> 반환
│       ├── watchlist/
│       │   ├── AddToWatchlistUseCase.kt      # 관심공고 등록 (Room + Supabase 동기화)
│       │   ├── RemoveFromWatchlistUseCase.kt # 관심공고 삭제
│       │   ├── GetWatchlistUseCase.kt        # 관심공고 목록 Flow<List> 반환
│       │   └── ObserveWatchlistRealtimeUseCase.kt # Realtime 구독 시작/종료
│       └── auth/
│           ├── SignInWithGoogleUseCase.kt
│           └── SignOutUseCase.kt
│
└── presentation/
    ├── navigation/
    │   ├── AppNavGraph.kt               # NavHost 전체 라우팅 정의
    │   └── Screen.kt                    # sealed class 라우트 상수
    │
    ├── common/
    │   ├── component/
    │   │   ├── BidNoticeCard.kt         # 공고 카드 공통 컴포넌트
    │   │   ├── BidStatusBadge.kt        # 상태 배지 (변경/취소/개찰 색상 구분)
    │   │   ├── LoadingIndicator.kt
    │   │   ├── ErrorView.kt
    │   │   └── EmptyView.kt
    │   └── theme/
    │       ├── Color.kt
    │       ├── Theme.kt
    │       └── Type.kt
    │
    ├── splash/
    │   └── SplashScreen.kt              # 세션 확인 후 분기 (로그인 or 메인)
    │
    ├── login/
    │   ├── LoginScreen.kt
    │   └── LoginViewModel.kt
    │
    ├── bid/
    │   ├── list/
    │   │   ├── BidListScreen.kt         # 공사/용역/물품 탭 + 무한 스크롤
    │   │   └── BidListViewModel.kt
    │   ├── detail/
    │   │   ├── BidDetailScreen.kt       # 공고 상세 + 관심등록 토글
    │   │   └── BidDetailViewModel.kt
    │   └── search/
    │       ├── SearchScreen.kt          # 키워드 + 날짜 범위 + 업종 필터
    │       └── SearchViewModel.kt
    │
    ├── watchlist/
    │   ├── WatchlistScreen.kt           # Realtime 구독 기반 실시간 상태 표시
    │   └── WatchlistViewModel.kt
    │
    ├── result/
    │   ├── BidResultListScreen.kt
    │   └── BidResultListViewModel.kt
    │
    ├── notification/
    │   ├── NotificationListScreen.kt    # Room 기반 수신 알림 히스토리
    │   └── NotificationListViewModel.kt
    │
    └── settings/
        ├── SettingsScreen.kt
        └── SettingsViewModel.kt
```

---

## 3. 주요 라이브러리 목록 및 최신 안정 버전

버전 관리 방식: `gradle/libs.versions.toml` (Version Catalog)을 도입하여 루트에서 일괄 관리합니다. 아래 버전은 2026-04-22 기준 최신 안정 버전이며, 구현 시 최신 버전을 재확인하세요.

### Android / Kotlin 기반

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| AGP (Android Gradle Plugin) | 8.9.2 | 빌드 도구 (alpha → 안정 버전으로 교체 권장) |
| Kotlin | 2.1.20 | 언어 |
| Compose BOM | 2025.04.01 | Compose 라이브러리 버전 통합 관리 |
| androidx.core:core-ktx | 1.16.0 | Kotlin 확장 |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.9.0 | Lifecycle 관리 |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.9.0 | ViewModel + Compose 연동 |
| androidx.activity:activity-compose | 1.10.1 | ComponentActivity + Compose |
| androidx.compose.material3 | (BOM 포함) | Material3 UI 컴포넌트 |

> **주의:** 현재 `build.gradle.kts`의 AGP `9.0.0-alpha06`은 실험적 버전입니다. 안정 버전인 8.9.x로 다운그레이드하거나, 9.0.0 정식 출시 후 마이그레이션을 권장합니다.

### 네트워크

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| com.squareup.retrofit2:retrofit | 2.11.0 | 조달청 API HTTP 클라이언트 |
| com.squareup.retrofit2:converter-gson | 2.11.0 | JSON 응답 역직렬화 |
| com.squareup.okhttp3:okhttp | 4.12.0 | OkHttp 코어 |
| com.squareup.okhttp3:logging-interceptor | 4.12.0 | 디버그 네트워크 로그 |

### DI

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| com.google.dagger:hilt-android | 2.52 | 의존성 주입 |
| com.google.dagger:hilt-android-compiler | 2.52 | Hilt 어노테이션 프로세서 |
| androidx.hilt:hilt-navigation-compose | 1.2.0 | NavGraph + Hilt ViewModel 주입 |

### 로컬 DB

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| androidx.room:room-runtime | 2.7.1 | SQLite ORM |
| androidx.room:room-ktx | 2.7.1 | 코루틴 + Flow 확장 |
| androidx.room:room-paging | 2.7.1 | Paging3 연동 (RemoteMediator 미사용 시에도 필요) |
| androidx.room:room-compiler | 2.7.1 | KSP 어노테이션 프로세서 |

> Room 컴파일러는 **KAPT 대신 KSP** 사용을 강력 권장합니다. 빌드 속도가 유의미하게 개선됩니다.

### 페이징

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| androidx.paging:paging-runtime | 3.3.6 | Paging3 코어 |
| androidx.paging:paging-compose | 3.3.6 | LazyColumn + PagingData 연동 |

### 내비게이션

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| androidx.navigation:navigation-compose | 2.9.0 | Compose 화면 전환 및 Back Stack 관리 |

### 이미지

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| io.coil-kt.coil3:coil-compose | 3.1.0 | 기관 로고 등 이미지 비동기 로딩 |

> Coil3는 OkHttp를 네트워크 엔진으로 공유할 수 있어 Retrofit의 OkHttpClient 인스턴스를 재활용하면 커넥션 풀이 공유됩니다.

### 인증 — Google 소셜 로그인

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| androidx.credentials:credentials | 1.5.0 | Credential Manager API |
| androidx.credentials:credentials-play-services-auth | 1.5.0 | Google Identity 서비스 연동 |
| com.google.android.libraries.identity.googleid:googleid | 1.1.1 | Google ID Token 획득 |

> 기존 `GoogleSignInClient`(deprecated)는 사용하지 않습니다. **Credential Manager API**로 Google ID Token을 획득한 뒤 Supabase `signInWithIdToken()`에 전달합니다.

### FCM

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| com.google.firebase:firebase-bom | 33.8.0 | Firebase 라이브러리 버전 통합 |
| com.google.firebase:firebase-messaging-ktx | (BOM 포함) | FCM 메시지 수신 처리 |

### Supabase Android SDK

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| io.github.jan-tennert.supabase:bom | 3.1.4 | Supabase KMP BOM |
| io.github.jan-tennert.supabase:auth-kt | (BOM 포함) | 인증 (Google OIDC 연동) |
| io.github.jan-tennert.supabase:postgrest-kt | (BOM 포함) | DB CRUD |
| io.github.jan-tennert.supabase:realtime-kt | (BOM 포함) | WebSocket Realtime 구독 |
| io.ktor:ktor-client-android | 3.1.2 | Supabase SDK HTTP 엔진 |

### 직렬화 및 유틸

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.8.0 | Supabase SDK 내부 직렬화 필수 |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.10.1 | 비동기 처리 |
| com.google.code.gson:gson | 2.11.0 | Retrofit 응답 파싱 |
| com.jakewharton.timber:timber | 5.0.1 | 구조화된 로그 (DEBUG only) |

> Retrofit은 Gson, Supabase SDK는 kotlinx.serialization을 각각 사용합니다. 두 직렬화 라이브러리가 공존하지만, 역할 범위가 계층별로 분리되어 충돌하지 않습니다.

---

## 4. 화면 목록 및 각 화면의 역할

### Bottom Navigation 탭 구성

```
[입찰공고] [관심공고] [낙찰결과] [알림] [설정]
```

### 화면 상세

| # | 화면명 | 라우트 | 역할 |
|---|---|---|---|
| 1 | **SplashScreen** | `splash` | 앱 시작 시 Supabase 세션 유효성 확인 → 로그인 or 메인으로 분기. FCM 토큰 갱신 트리거. |
| 2 | **LoginScreen** | `login` | Google Credential Manager 실행 → ID Token 획득 → Supabase Auth 처리. 로그인 성공 시 BidListScreen으로 이동. |
| 3 | **BidListScreen** | `bid/list?category={category}` | 공사/용역/물품 탭 전환, Paging3 LazyColumn 무한 스크롤. 탭별 독립 PagingSource. 날짜 범위 간편 필터(오늘/1주/1개월). |
| 4 | **SearchScreen** | `bid/search` | 키워드 + 날짜 범위(inqryBgnDt~inqryEndDt) + 업종 복합 필터. 결과는 BidNoticePagingSource를 파라미터만 달리 하여 재활용. |
| 5 | **BidDetailScreen** | `bid/detail/{bidNtceNo}` | 공고 상세 정보(추정가격·마감일시·계약방법 등) 표시. 관심공고 등록/해제 토글 버튼. `bidNtceDtlUrl`로 나라장터 외부 브라우저 열기. |
| 6 | **WatchlistScreen** | `watchlist` | 관심공고 목록, Supabase Realtime 구독으로 상태 배지 실시간 갱신(변경/취소/개찰). 스와이프 제스처로 관심 해제. |
| 7 | **BidResultListScreen** | `result` | 낙찰결과 목록(낙찰업체명·낙찰금액·낙찰률·참가업체수). 업종별 탭. Paging3. |
| 8 | **NotificationListScreen** | `notifications` | 수신된 FCM Push 알림 이력. Room 로컬 저장 기반. 읽음/안읽음 구분. 탭 시 해당 공고 상세로 이동. |
| 9 | **SettingsScreen** | `settings` | 관심 업종 필터, 알림 ON/OFF(변경/취소/개찰 개별 설정), 계정 로그아웃. |

---

## 5. Supabase DB 테이블 설계

> Free 플랜 제약: DB 용량 500MB, Realtime 최대 동시 연결 200개, Row Level Security 필수 활성화.

### 5-1. `users`

Supabase Auth의 `auth.users`와 연동되는 공개 프로필 테이블.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK, FK → auth.users.id | Supabase Auth UID와 동일 |
| `email` | text | NOT NULL | Google 계정 이메일 |
| `display_name` | text | | 표시 이름 |
| `fcm_token` | text | | 현재 기기 FCM 등록 토큰 |
| `created_at` | timestamptz | DEFAULT now() | 가입일시 |
| `updated_at` | timestamptz | DEFAULT now() | 최종 업데이트 일시 |

RLS 정책: `auth.uid() = id` 조건으로 본인 row만 SELECT/UPDATE 허용.

---

### 5-2. `bid_notices`

사용자별 관심공고 저장 테이블. GitHub Actions 폴링 스크립트가 이 테이블을 읽어 상태 변경을 감지하고, 변경 시 직접 UPDATE합니다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK, DEFAULT gen_random_uuid() | |
| `user_id` | uuid | FK → users.id, NOT NULL | 소유 사용자 |
| `bid_ntce_no` | text | NOT NULL | 입찰공고번호 (조달청 원본값) |
| `bid_ntce_nm` | text | NOT NULL | 공고명 |
| `ntce_instt_nm` | text | | 공고기관명 |
| `dminstt_nm` | text | | 수요기관명 |
| `bid_ntce_dt` | timestamptz | | 공고일시 |
| `bid_clse_dt` | timestamptz | | 마감일시 |
| `openg_dt` | timestamptz | | 개찰일시 |
| `presmpt_prce` | bigint | | 추정가격 (원) |
| `bdgt_amt` | bigint | | 예산금액 (원) |
| `bid_category` | text | NOT NULL | 업종: CNSTWK / SERVC / THNG |
| `current_status` | text | DEFAULT 'REGISTERED' | 현재 공고 상태 |
| `bid_ntce_dtl_url` | text | | 나라장터 상세 URL |
| `last_checked_at` | timestamptz | | GitHub Actions 마지막 폴링 시각 |
| `watched_at` | timestamptz | DEFAULT now() | 관심등록 일시 |

UNIQUE 제약: `(user_id, bid_ntce_no)` → 동일 공고 중복 등록 방지.  
Realtime 활성화: `bid_notices` 테이블에 Realtime 활성화 → `current_status` UPDATE 시 앱이 이벤트 수신.  
RLS 정책: 본인 row만 SELECT/INSERT/DELETE 허용. GitHub Actions는 `service_role` 키로 RLS 우회하여 UPDATE 가능.

---

### 5-3. `bid_notice_status_history`

관심공고 상태 변경 이력. GitHub Actions 스크립트가 변경 감지 시 INSERT합니다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK, DEFAULT gen_random_uuid() | |
| `bid_notice_id` | uuid | FK → bid_notices.id, NOT NULL | 대상 관심공고 |
| `previous_status` | text | NOT NULL | 변경 전 상태 |
| `new_status` | text | NOT NULL | 변경 후 상태 |
| `ntce_kind_nm` | text | | 조달청 원본 ntceKindNm 값 |
| `detected_at` | timestamptz | DEFAULT now() | 변경 감지 일시 |

RLS 정책: 사용자는 본인 공고의 이력만 SELECT 가능. INSERT는 service_role 전용.

---

## 6. GitHub Actions 워크플로우 설계

### 6-1. 파일 위치

```
.github/workflows/bid-polling.yml     ← 워크플로우 정의
scripts/poll-bid-status.js            ← 실행 스크립트 (Node.js)
scripts/package.json                  ← @supabase/supabase-js, node-fetch 등 의존성
```

### 6-2. `bid-polling.yml` 구조 설명

| 항목 | 내용 |
|---|---|
| 트리거 | `schedule: cron: "0 * * * *"` (1시간 간격) |
| 실행 환경 | `ubuntu-latest`, Node.js 20 |
| 주요 Steps | Checkout → Node.js 설치 → `npm ci` → `node scripts/poll-bid-status.js` |
| Secrets 참조 | `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `GOGOV_API_KEY`, `FCM_SERVER_KEY` |

> GitHub Actions Free 플랜은 월 2,000분 제공. 10분 간격 = 하루 144회 = 월 약 4,320분으로 **Free 플랜 초과**. **유료 플랜 전환 또는 실행 주기를 15~30분으로 조정**을 검토해야 합니다. 대안으로 GitHub Actions 대신 무료 Cron 서비스(cron-job.org, Railway Free Tier 등)를 검토할 수 있습니다.

### 6-3. `poll-bid-status.js` 스크립트 역할 및 단계별 흐름

**Step 1 — 관심공고 목록 수집**
- Supabase `service_role` 키로 `bid_notices` 테이블 전체 조회 (RLS 우회)
- `bid_ntce_no`, `bid_category`, `current_status`, `user_id` 수집

**Step 2 — 조달청 API 상태 확인**
- `bid_ntce_no` 단위로 `BidPublicInfoService` 호출 (`inqryDiv=2`)
- 응답의 `ntceKindNm` 값으로 현재 상태 판별:
  - "등록" → `REGISTERED`
  - "변경" → `CHANGED`
  - "취소" → `CANCELLED`
  - "재공고" → `REOPENED`
- 별도로 `getOpengResultListInfoOpengCompt` 호출하여 개찰완료 여부 확인 → `OPENED`
- 요청 간 50ms delay 삽입 (30 tps 제한 대응)

**Step 3 — 변경 감지 및 DB 업데이트**
- `current_status`와 API 응답 상태 비교
- 상이하면:
  - `bid_notices.current_status`, `last_checked_at` UPDATE → Realtime 이벤트 자동 발생
  - `bid_notice_status_history` INSERT
  - 해당 공고 소유자의 `fcm_token` 조회 후 FCM HTTP v1 API 호출

**Step 4 — Supabase 활성 상태 유지 (Ping)**
- 변경 감지 여부와 무관하게 매 실행 시 `bid_notices` 테이블에 경량 SELECT 쿼리 실행
- Supabase Free 플랜의 **1주일 미사용 시 자동 일시정지** 방지

### 6-4. GitHub Actions Secrets 목록

| Secret 키 | 용도 |
|---|---|
| `SUPABASE_URL` | Supabase 프로젝트 URL |
| `SUPABASE_SERVICE_ROLE_KEY` | RLS 우회용 서버 전용 키 (앱에 절대 포함 금지) |
| `GOGOV_API_KEY` | 조달청 공공데이터포털 ServiceKey |
| `FCM_SERVER_KEY` | FCM HTTP v1 API 서버 키 |

---

## 7. API 공통 Interceptor 설계

### 7-1. `AuthInterceptor` (OkHttp Application Interceptor)

조달청 API 모든 요청에 공통 파라미터를 자동 주입합니다.

**주입 대상:**
- `ServiceKey`: 공공데이터포털 발급 인증키
- `type`: 항상 `json` 고정

**동작 방식:**
1. 원본 Request의 URL을 `HttpUrl.newBuilder()`로 복사
2. `addEncodedQueryParameter("ServiceKey", key)` 및 `addQueryParameter("type", "json")` 추가  
   (`encoded=true` 필수: ServiceKey는 `+`, `=` 등 특수문자 포함 시 이중 인코딩 방지)
3. 수정된 URL로 Request 재구성 → 체인에 전달

**보안 고려사항:**
- `ServiceKey`는 `local.properties`에서 `BuildConfig`로 주입하고 Proguard 난독화 적용
- `HttpLoggingInterceptor`는 DEBUG 빌드 전용, RELEASE 빌드에서 반드시 제거

---

### 7-2. `RetryInterceptor` (OkHttp Application Interceptor)

조달청 API의 30 tps 제한 대응 재시도 전략입니다.

**전략: 지수 백오프 Retry**

| 시도 | 대기 시간 |
|---|---|
| 1차 (원본 요청) | 즉시 |
| 2차 재시도 | 1,000ms |
| 3차 재시도 | 2,000ms |
| 4차 재시도 | 4,000ms |
| 4회 초과 | LoadState.Error로 전파 |

**동작 조건:**
- HTTP 429 응답 수신 시 재시도
- `Retry-After` 헤더가 있으면 해당 값 우선 사용
- HTTP 5xx 서버 에러 시 동일 전략 적용

**Paging3 연동 고려사항:**
- `PagingConfig(pageSize=20, prefetchDistance=2)`로 prefetch 최소화
- 동시 요청 수를 줄이기 위해 Paging3의 기본 `initialLoadSize`를 `pageSize`와 동일하게 설정
- `numOfRows=20` 고정으로 API 호출당 부하 최소화

---

## 8. 예상 기술 난관 및 해결 방향

### 8-1. 조달청 API ServiceKey 이중 인코딩

**문제:** ServiceKey에 포함된 `+`, `/`, `=` 특수문자를 OkHttp `addQueryParameter()`가 URL 인코딩하면, 서버에서 이중 디코딩하여 인증 실패가 발생합니다.

**해결:** `HttpUrl.Builder.addEncodedQueryParameter()` 사용. Retrofit 인터페이스에서는 `@Query(value = "ServiceKey", encoded = true)`. Interceptor에서 일괄 처리하므로 API 인터페이스는 ServiceKey 파라미터를 선언하지 않습니다.

---

### 8-2. 조달청 API 응답 구조 사전 검증 필요

**문제:** 공사/용역/물품 오퍼레이션의 실제 JSON 응답 필드명이 문서 명세와 다를 수 있습니다. 특히 `items` 배열이 단건일 때 배열 대신 객체로 반환되는 공공 API 공통 패턴이 존재합니다.

**해결:** 구현 전 Postman 또는 `curl`로 각 오퍼레이션의 실제 응답 JSON을 수집합니다. `items`가 배열/객체 혼용인 경우 Gson의 커스텀 `TypeAdapter`를 작성하여 단건 객체를 리스트로 정규화합니다.

---

### 8-3. GitHub Actions Free 플랜 사용량 초과

**문제:** 10분 간격 Cron은 월 약 4,320분 소모 → GitHub Free 플랜 2,000분 초과.

**해결 옵션:**
- Option A: 주기를 30분으로 늘림 → 월 1,440분, Free 플랜 내 운영 가능. 실시간성은 Realtime으로 보완.
- Option B: GitHub Pro 플랜 전환 (월 4달러).
- Option C: `cron-job.org` 무료 Cron 서비스로 외부 HTTP 엔드포인트 트리거 → Supabase Edge Function 또는 별도 서버리스 호출.

---

### 8-4. Supabase Realtime 구독 배터리 소모 및 연결 끊김

**문제:** WebSocket 상시 유지는 배터리 소모가 있고, 앱 백그라운드 전환 시 Android 시스템이 연결을 끊을 수 있습니다.

**해결:**
- `DefaultLifecycleObserver`를 구현한 `SupabaseRealtimeService`에서 `onStart()` 시 구독, `onStop()` 시 해제
- 백그라운드에서는 FCM Push를 통해 상태 전달 (Realtime 불필요)
- 포그라운드 복귀 시 REST API로 관심공고 목록 상태를 일회성 동기화 후 Realtime 재구독

---

### 8-5. Google 로그인 → Supabase 연동 토큰 흐름

**문제:** Credential Manager로 얻은 Google ID Token의 유효 기간(1시간)과 Supabase JWT 세션 관리 방식이 독립적으로 동작하므로, 세션 갱신 흐름을 명확히 설계하지 않으면 인증 오류가 발생합니다.

**해결:**
- Credential Manager → `GoogleIdTokenCredential.idToken` 획득
- `SupabaseClient.auth.signInWithIdToken(Google, idToken)` 호출 → Supabase가 자체 access_token + refresh_token 발급
- 이후 인증은 Supabase 세션만으로 처리 (Google ID Token 재사용 불필요)
- 앱 재시작 시 `auth.currentSession` null 여부만 확인 → null이면 LoginScreen, 유효하면 메인

---

### 8-6. 관심공고 수 증가에 따른 폴링 API 호출량 증가

**문제:** 사용자당 관심공고가 많아지고 사용자 수가 늘어나면, 폴링 스크립트의 조달청 API 호출 횟수가 비례적으로 증가하여 30 tps를 초과할 위험이 있습니다.

**해결:**
- 동일 `bid_ntce_no`를 여러 사용자가 구독한 경우, API 호출을 공고 단위로 중복 제거(Deduplication)하고 결과를 공유
- 요청 간 50ms 강제 지연으로 최대 20 req/s 유지
- 공고 상태가 `OPENED` 또는 `CANCELLED`인 경우 더 이상 폴링 대상에서 제외 (최종 상태 처리)

---

## 부록: 개발 단계별 우선순위

| Phase | 작업 내용 | 산출물 |
|---|---|---|
| 1 | 프로젝트 리셋 (패키지명, Version Catalog, AGP 안정화, Hilt 설정) | 빌드 성공 |
| 2 | 조달청 API 연동 (Retrofit, Interceptor, PagingSource, BidListScreen) | 공고 목록 표시 |
| 3 | SearchScreen + BidDetailScreen 구현 | 검색 및 상세 조회 |
| 4 | Google 로그인 + Supabase Auth 연동 | 인증 완료 |
| 5 | 관심공고 기능 (Room 로컬 + Supabase bid_notices 동기화) | WatchlistScreen |
| 6 | GitHub Actions 폴링 스크립트 + FCM 발송 | 상태 변경 감지 알림 |
| 7 | Supabase Realtime 구독 → WatchlistScreen 실시간 반영 | 실시간 UI |
| 8 | 낙찰결과, 알림 히스토리, 설정 화면 | 전체 화면 완성 |
| 9 | Proguard, 릴리즈 빌드, 성능 측정 | 배포 준비 |