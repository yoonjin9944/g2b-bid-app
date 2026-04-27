# G2B 입찰공고 모니터링 앱 — 구현 계획서

> 작성일: 2026-04-23  
> 기반 문서: research.md (2026-04-22)  
> 환경: compileSdk 36 / minSdk 33 / AGP 8.9.2 (alpha → 안정 교체) / Kotlin 2.1.20  
> 상태: **승인 대기 — 이 문서 검토 후 구현 시작**

---

## Figma 디자인 참조 원칙

> **Claude Code에서 UI 구현 시 반드시 준수할 사항**

1. 각 Phase의 **Figma 참조** 섹션에 명시된 링크를 MCP를 통해 직접 조회하여 디자인 컨텍스트를 가져온다.
2. Figma 디자인을 구현의 **1차 기준**으로 삼는다. 단, 아래 경우에는 **코드 생성 전 반드시 질문**한다:
   - Figma 디자인과 이 계획서의 구조/흐름이 **불일치**하는 경우
   - Figma에 명시되지 않은 상태(로딩, 에러, 빈 목록 등) UI가 필요한 경우
   - 컴포넌트 재사용 방식이나 네이밍이 **계획서와 다른** 경우
   - 디자인 의도가 **모호**하거나 여러 해석이 가능한 경우
3. 질문 없이 임의로 디자인을 변경하거나 계획서 구조를 수정하지 않는다.
4. 브랜드 색상은 **부록: 브랜드 색상 정의** 섹션을 따른다.

---

## 기능 흐름 요약

### 흐름 0: 앱 시작 → 버전 체크 → 로그인 분기

```
앱 시작
  └─ SplashScreen (SplashViewModel이 UiState 관리)
       ├─ GitHub Pages JSON 버전 체크 (VersionCheckRepository / Dispatchers.IO)
       │    ├─ FORCE_UPDATE  → 업데이트 다이얼로그 (취소 불가) → APK 다운로드/설치
       │    ├─ RECOMMEND_UPDATE → 업데이트 다이얼로그 (나중에 선택 가능) → 분기
       │    ├─ UP_TO_DATE   → 다음 단계 진행
       │    └─ ERROR        → 다음 단계 진행 (버전 체크 실패는 blocking하지 않음)
       └─ Supabase 세션 확인
            ├─ 세션 없음 → LoginScreen
            └─ 세션 있음 → BidListScreen (메인)
```

### 흐름 1: 신규 입찰공고 검색 및 관심공고 등록

```
앱 진입 → 권한 설정(INTERNET, POST_NOTIFICATIONS) → 로그인
  → BidListScreen (메인 / Bottom Nav 첫 번째 탭)
  → SearchScreen (통합 / 상세 검색)
  → BidListScreen 결과 출력 (Paging3 LazyColumn)
  → [재검색] 또는 [항목 선택] 또는 [관심공고 토글]
  → BidDetailScreen (전체 크기 팝업 형태)
  → 관심공고 등록 토글 → Supabase bid_notices INSERT + Room 캐싱
```

### 흐름 2: 관심공고 목록 조회 및 상세 출력

```
앱 진입 → BidListScreen
  → WatchlistScreen (Bottom Nav 두 번째 탭 또는 목록 항목 클릭)
  → 목록 내 재검색 또는 스와이프 삭제
  → 항목 선택 → BidDetailScreen (상세 팝업)
  → Supabase Realtime 구독으로 current_status 실시간 갱신
```

---

## 1. 개발 Phase 순서

### Phase 1 — 프로젝트 세팅
**완료 기준:** 앱이 빈 화면으로 빌드 및 실행됨. Hilt 주입 동작 확인.

| 작업 항목 | 내용 |
|---|---|
| 패키지명 변경 | `com.example.g2b_bid_app` → `com.g2b.bidapp` |
| AGP 버전 교체 | `9.0.0-alpha06` → `8.9.2` (안정 버전) |
| Version Catalog 도입 | `gradle/libs.versions.toml` 생성. 전체 라이브러리 버전 중앙화 |
| KSP 설정 | KAPT 제거, KSP로 전환 (Room, Hilt 모두 KSP 사용) |
| Hilt 설정 | `G2bApplication.kt` (@HiltAndroidApp), `MainActivity`에 `@AndroidEntryPoint` |
| 기본 DI 모듈 뼈대 | `NetworkModule`, `DatabaseModule`, `RepositoryModule`, `SupabaseModule` 빈 파일 생성 |
| Navigation 뼈대 | `Screen.kt` sealed class, `AppNavGraph.kt` NavHost 뼈대 |
| 테마 세팅 | `Color.kt`에 브랜드 색상 등록. `Theme.kt` Material3 색상 시스템 연결 |
| Retrofit / OkHttp 기본 설정 | `NetworkModule`에 OkHttpClient + Retrofit 인스턴스 제공 (Interceptor는 Phase 2에서 추가) |
| Supabase 클라이언트 초기화 | `SupabaseModule`에 `SupabaseClient` 제공 (Ktor Android 엔진 사용) |

---

### Phase 2 — 버전 체크 및 강제/권장 업데이트
**완료 기준:** 버전 JSON 응답에 따라 FORCE/RECOMMEND/UP_TO_DATE 분기가 정확히 동작하고, APK 다운로드 후 설치 Intent가 실행됨. Figma 디자인과 시각적으로 일치.

**Figma 참조**

| 화면 | Figma 링크 |
|---|---|
| SplashScreen (로딩 / ForceUpdate 다이얼로그 / RecommendUpdate 다이얼로그 / 다운로드 프로그레스) | https://www.figma.com/design/7u1WMffio6O0giNNLxCOKN/g2b-bid-app?node-id=15-7681 |

| 작업 항목 | 내용 |
|---|---|
| GitHub Pages 설정 | 별도 Repository (또는 docs/ 폴더) 에 `version.json` 호스팅. 내용 구조는 섹션 8 참조 |
| `VersionCheckRepository` 구현 | OkHttp로 GitHub Pages URL GET 요청. Dispatchers.IO에서 실행. 응답 파싱 후 판단 결과 반환 |
| `SplashViewModel` UiState 정의 | Loading / ForceUpdate / RecommendUpdate / UpToDate / Error 상태 관리 |
| `SplashScreen` 구현 | UiState 관찰 → 분기 UI 표시 |
| 강제 업데이트 다이얼로그 | 취소 버튼 없음. 확인 누르면 APK 다운로드 진행 |
| 권장 업데이트 다이얼로그 | "지금 업데이트" / "나중에" 버튼. 나중에 선택 시 버전 체크 건너뜀 |
| APK 다운로드 | OkHttp로 스트리밍 다운로드 → `getExternalFilesDir(DIRECTORY_DOWNLOADS)` 저장. 다운로드 진행률 Flow로 방출 |
| APK 설치 | FileProvider URI 생성 → `ACTION_VIEW` Intent 실행 |
| `AndroidManifest.xml` 추가 항목 | `INTERNET`, `REQUEST_INSTALL_PACKAGES` 권한. `<provider>` (FileProvider) 선언 |
| `file_provider_paths.xml` | `res/xml/` 위치. `external-files-path` 경로 설정 |
| Supabase 세션 확인 | 버전 체크 완료 후 `auth.currentSession` null 여부 확인 → 화면 분기 |

---

### Phase 3 — 로그인 (Google OAuth + Supabase Auth)
**완료 기준:** Google 로그인 완료 후 Supabase 세션이 생성되고 BidListScreen으로 이동. Figma 디자인과 시각적으로 일치.

**Figma 참조**

| 화면 | Figma 링크 |
|---|---|
| LoginScreen (Google 버튼 / Kakao 버튼 / Guest Mode) | https://www.figma.com/design/7u1WMffio6O0giNNLxCOKN/g2b-bid-app?node-id=15-7696 |

| 작업 항목 | 내용 |
|---|---|
| `LoginScreen` 완성 | 기존 Figma 디자인 기반 UI (이미 생성됨). 버튼 클릭 → ViewModel 액션 호출 |
| `LoginViewModel` 구현 | Credential Manager 실행 → ID Token 획득 → UseCase 호출 → UiState 반환 |
| `SignInWithGoogleUseCase` | Credential Manager API 래핑. Google ID Token 반환 |
| `AuthRepositoryImpl` | `supabase.auth.signInWithIdToken(Google, idToken)` 호출. 세션 저장 |
| Supabase Google OAuth 설정 | Supabase 대시보드에서 Google Provider 활성화 필요 (설정 가이드 포함) |
| FCM 토큰 등록 | 로그인 성공 후 `FirebaseMessaging.getInstance().token` 획득 → `users.fcm_token` Upsert |
| 게스트 모드 처리 | LoginScreen Footer의 "Guest Mode" → 읽기 전용 BidListScreen으로 이동 (관심공고 기능 비활성화) |

---

### Phase 4 — 입찰공고 검색 및 목록 (Paging3 + API 연동)
**완료 기준:** 키워드/날짜/업종 필터로 검색 시 공고 목록이 Paging3 LazyColumn으로 표시됨. Figma 디자인과 시각적으로 일치.

**Figma 참조**

| 화면 | Figma 링크 |
|---|---|
| BidListScreen 메인 (탭 / 카드 목록 / 로딩·에러·빈 상태) | https://www.figma.com/design/7u1WMffio6O0giNNLxCOKN/g2b-bid-app?node-id=15-7988 |
| SearchScreen (통합 검색 / 상세 필터) | https://www.figma.com/design/7u1WMffio6O0giNNLxCOKN/g2b-bid-app?node-id=15-7714 |
| 입찰공고 목록조회 | https://www.figma.com/design/7u1WMffio6O0giNNLxCOKN/g2b-bid-app?node-id=15-7835 |

| 작업 항목 | 내용 |
|---|---|
| `BidPublicInfoApi` Retrofit 인터페이스 | 공사/용역/물품 오퍼레이션 선언 |
| `AuthInterceptor` (CommonParamInterceptor) | ServiceKey + type=json 자동 주입. `addEncodedQueryParameter` 사용 |
| `RetryInterceptor` | 429/5xx 지수 백오프 재시도 (최대 3회) |
| `BidNoticePagingSource` | pageNo / numOfRows(20) 관리. `totalCount` 기반 마지막 페이지 판단 |
| `GetBidNoticeListUseCase` | 검색 파라미터 → `Flow<PagingData<BidNotice>>` 반환 |
| `BidListScreen` | 공사/용역/물품 탭, LazyColumn + `collectAsLazyPagingItems()`, 로딩/에러/빈 상태 처리 |
| `SearchScreen` | 키워드, 날짜범위, 업종 복합 필터 UI. 결과를 BidListScreen에 파라미터로 전달 |
| DTO → Domain Mapper | `BidNoticeDto` → `BidNotice` 변환. `items` 단건/배열 혼용 대응 TypeAdapter |
| API 실제 응답 검증 | Postman으로 각 오퍼레이션 실제 JSON 수집 후 DTO 필드명 확정 (구현 전 선행 필수) |

---

### Phase 5 — 공고 상세 팝업
**완료 기준:** 목록 항목 탭 시 BidDetailScreen이 전체 크기 팝업으로 열리고 외부 브라우저 연결이 동작. Figma 디자인과 시각적으로 일치.

**Figma 참조**

| 화면 | Figma 링크 |
|---|---|
| BidDetailScreen (전체 크기 팝업 / 관심공고 토글 / 외부 링크) | https://www.figma.com/design/7u1WMffio6O0giNNLxCOKN/g2b-bid-app?node-id=15-7579 |

| 작업 항목 | 내용 |
|---|---|
| `BidDetailScreen` | ModalBottomSheet 또는 Dialog 형태의 전체 크기 팝업. 공고 전체 필드 표시 |
| 관심공고 토글 버튼 | 등록/해제 아이콘 버튼. 로그인 사용자만 활성화 |
| 나라장터 외부 열기 | `bidNtceDtlUrl` → CustomTabs 또는 외부 브라우저 Intent |
| 금액 포맷팅 | `presmptPrce` (원) → 억/만원 단위 표시 유틸 함수 |

---

### Phase 6 — 관심공고 등록/삭제 (Supabase DB + Room 로컬 캐싱)
**완료 기준:** 관심공고 등록/삭제가 Room과 Supabase에 동시 반영되고 오프라인 시 Room에서 로컬 데이터 표시.

| 작업 항목 | 내용 |
|---|---|
| Room Entity 및 DAO | `WatchedBidEntity`, `BidStatusHistoryEntity`, 각 DAO 구현 |
| `WatchlistRepositoryImpl` | 등록: Room INSERT 선행 → Supabase INSERT. 삭제: Supabase DELETE → Room DELETE |
| `AddToWatchlistUseCase` | 중복 체크(Room 기준) → 저장 |
| `RemoveFromWatchlistUseCase` | 관심공고 ID 기반 삭제 |
| `GetWatchlistUseCase` | Room `Flow<List<WatchedBid>>` 반환 (Room이 단일 소스) |
| 동기화 전략 | 앱 시작 시 Supabase `bid_notices` 전체 조회 → Room과 diff 비교 → 누락분 INSERT |

---

### Phase 7 — 관심공고 목록 화면
**완료 기준:** WatchlistScreen에서 목록 조회, 검색, 스와이프 삭제, 상세 팝업 진입이 모두 동작. Figma 디자인과 시각적으로 일치.

**Figma 참조**

| 화면 | Figma 링크 |
|---|---|
| WatchlistScreen (목록 / 스와이프 삭제 / BidStatusBadge) | https://www.figma.com/design/7u1WMffio6O0giNNLxCOKN/g2b-bid-app?node-id=15-7430 |

| 작업 항목 | 내용 |
|---|---|
| `WatchlistScreen` | Room Flow 구독 기반 LazyColumn. `BidStatusBadge` 컴포넌트로 상태 시각화 |
| 목록 내 검색 | 공고명 키워드 Room 쿼리 필터 |
| 스와이프 삭제 | `SwipeToDismissBox` 적용. 삭제 확인 Snackbar (실행취소 지원) |
| `BidStatusBadge` | REGISTERED(기본) / CHANGED(주황) / CANCELLED(빨강) / REOPENED(파랑) / OPENED(회색) 색상 구분 |
| 상태 업데이트 연결 | Phase 8 완료 후 Realtime 구독으로 실시간 갱신. Phase 7 단계에서는 REST 동기화만 구현 |

---

### Phase 8 — Push 알림 (GitHub Actions Cron + FCM + Supabase Realtime)
**완료 기준:** 관심공고 상태 변경 시 FCM Push가 수신되고 WatchlistScreen 배지가 실시간으로 갱신.

| 작업 항목 | 내용 |
|---|---|
| Firebase 프로젝트 연동 | `google-services.json` 추가. FCM 초기화 |
| `G2bFirebaseMessagingService` | FCM 수신 → Room `NotificationEntity` INSERT → 알림 표시 |
| GitHub Actions 워크플로우 | `bid-polling.yml` 생성 (섹션 9 참조) |
| `poll-bid-status.js` | Node.js 스크립트 (섹션 9 참조) |
| Supabase Realtime 구독 | `SupabaseRealtimeService`: `onStart()`에서 구독, `onStop()`에서 해제 |
| `ObserveWatchlistRealtimeUseCase` | Realtime 이벤트 → Room 업데이트 → WatchlistScreen 자동 갱신 |
| `NotificationListScreen` | Room 기반 수신 알림 이력 목록. 읽음/안읽음 구분 |
| `SettingsScreen` | 알림 유형별 ON/OFF 설정 (변경/취소/개찰 개별), 로그아웃 |

---

## 2. 화면별 Composable 구조 및 상태 관리

### UiState 패턴 (전체 공통)

모든 ViewModel은 단일 `sealed interface XxxUiState`를 StateFlow로 방출한다.  
화면은 `collectAsStateWithLifecycle()`로 상태를 구독하고 when 분기로 UI를 결정한다.

---

### SplashScreen

```
SplashScreen
  └─ SplashViewModel (StateFlow<SplashUiState>)
       ├─ Loading          → 로고 + 프로그레스 인디케이터
       ├─ ForceUpdate      → ForceUpdateDialog (취소 불가)
       │    └─ DownloadProgress(Float) → 선형 프로그레스바
       ├─ RecommendUpdate  → RecommendUpdateDialog ("지금" / "나중에")
       ├─ UpToDate         → 세션 확인 후 Navigate
       └─ Error            → 세션 확인 후 Navigate (버전 체크 실패는 non-blocking)
```

---

### LoginScreen

```
LoginScreen
  └─ LoginViewModel (StateFlow<LoginUiState>)
       ├─ Idle             → Google 로그인 버튼, Kakao 로그인 버튼, Guest Mode
       ├─ Loading          → 버튼 비활성화 + 로딩 인디케이터
       ├─ Success          → Navigate to BidListScreen
       └─ Error(message)   → SnackBar 오류 메시지
```

---

### BidListScreen

```
BidListScreen
  ├─ TopAppBar: SearchBar (클릭 시 SearchScreen으로 이동)
  ├─ TabRow: [공사] [용역] [물품]
  └─ BidListViewModel (LazyPagingItems<BidNotice>)
       ├─ LazyColumn
       │    └─ BidNoticeCard
       │         ├─ 공고명, 공고기관, 마감일, 추정가격
       │         └─ 관심공고 아이콘 버튼 (하트)
       ├─ LoadingState     → CircularProgressIndicator (하단)
       ├─ ErrorState       → ErrorView + 재시도 버튼
       └─ EmptyState       → EmptyView
```

---

### SearchScreen

```
SearchScreen
  ├─ SearchViewModel (StateFlow<SearchUiState>)
  ├─ 키워드 입력 TextField
  ├─ 날짜 범위 선택 (DateRangePicker)
  ├─ 업종 선택 (공사 / 용역 / 물품 ChipGroup)
  └─ "검색" 버튼 → BidListScreen으로 SearchParams 전달 (NavBackStack 활용)
```

---

### BidDetailScreen (팝업)

```
BidDetailScreen (ModalBottomSheet / 전체 화면)
  └─ BidDetailViewModel (StateFlow<BidDetailUiState>)
       ├─ Loading          → 스켈레톤 UI
       ├─ Success(BidNotice)
       │    ├─ 공고 상세 필드 스크롤 뷰
       │    ├─ 관심공고 토글 버튼 (등록됨 / 미등록)
       │    └─ "나라장터에서 보기" 버튼 → CustomTabs
       └─ Error            → ErrorView
```

---

### WatchlistScreen

```
WatchlistScreen
  ├─ WatchlistViewModel (StateFlow<WatchlistUiState>)
  ├─ 검색 TextField (Room 필터 쿼리)
  └─ LazyColumn
       └─ SwipeToDismissBox(BidNoticeCard)
            ├─ BidStatusBadge (REGISTERED / CHANGED / CANCELLED / REOPENED / OPENED)
            └─ 탭 → BidDetailScreen 팝업
```

---

### NotificationListScreen

```
NotificationListScreen
  └─ NotificationListViewModel (Flow<List<NotificationItem>>)
       └─ LazyColumn
            └─ NotificationCard
                 ├─ 공고명, 변경 내용, 수신 시각
                 ├─ 읽음/안읽음 시각적 구분
                 └─ 탭 → BidDetailScreen 팝업
```

---

## 3. ViewModel — UseCase — Repository 연결 흐름

```
[UI Layer]           [Domain Layer]              [Data Layer]
                     
BidListViewModel
  → GetBidNoticeListUseCase
       → BidRepository (interface)
            ← BidRepositoryImpl
                 ├─ BidNoticePagingSource (Retrofit API 호출)
                 └─ BidNoticeMapper (DTO → Domain)

WatchlistViewModel
  → GetWatchlistUseCase
       → WatchlistRepository (interface)
            ← WatchlistRepositoryImpl
                 ├─ WatchedBidDao (Room, Flow 구독)
                 └─ Supabase postgrest-kt (동기화)

  → AddToWatchlistUseCase / RemoveFromWatchlistUseCase
       → WatchlistRepository
            ← WatchlistRepositoryImpl

  → ObserveWatchlistRealtimeUseCase
       → WatchlistRepository
            ← SupabaseRealtimeService (WebSocket 이벤트 → Room UPDATE)

LoginViewModel
  → SignInWithGoogleUseCase
       → AuthRepository (interface)
            ← AuthRepositoryImpl
                 ├─ Credential Manager API (Google ID Token)
                 └─ Supabase auth.signInWithIdToken()

SplashViewModel
  → VersionCheckRepository (domain 없음, 직접 주입)
       ← OkHttp JSON fetch (GitHub Pages)
  → AuthRepository.currentSession()
```

---

## 4. Room DB 스키마 (로컬 캐싱용)

### `watched_bids` 테이블

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | TEXT | PK | Supabase bid_notices.id와 동일 UUID 문자열 |
| `bid_ntce_no` | TEXT | NOT NULL | 입찰공고번호 |
| `bid_ntce_nm` | TEXT | NOT NULL | 공고명 |
| `ntce_instt_nm` | TEXT | | 공고기관명 |
| `dminstt_nm` | TEXT | | 수요기관명 |
| `bid_ntce_dt` | INTEGER | | 공고일시 (Unix Timestamp) |
| `bid_clse_dt` | INTEGER | | 마감일시 (Unix Timestamp) |
| `openg_dt` | INTEGER | | 개찰일시 (Unix Timestamp) |
| `presmpt_prce` | INTEGER | | 추정가격 (원) |
| `bdgt_amt` | INTEGER | | 예산금액 (원) |
| `bid_category` | TEXT | NOT NULL | CNSTWK / SERVC / THNG |
| `current_status` | TEXT | NOT NULL | REGISTERED / CHANGED / CANCELLED / REOPENED / OPENED |
| `bid_ntce_dtl_url` | TEXT | | 나라장터 상세 URL |
| `watched_at` | INTEGER | NOT NULL | 관심등록 일시 (Unix Timestamp) |
| `synced_at` | INTEGER | | Supabase 마지막 동기화 시각 |

### `bid_status_history` 테이블

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | TEXT | PK | UUID |
| `watched_bid_id` | TEXT | FK → watched_bids.id | |
| `previous_status` | TEXT | NOT NULL | 변경 전 상태 |
| `new_status` | TEXT | NOT NULL | 변경 후 상태 |
| `detected_at` | INTEGER | NOT NULL | 변경 감지 일시 |

### `notifications` 테이블

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | TEXT | PK | UUID |
| `watched_bid_id` | TEXT | | 관련 공고 ID (nullable: 일반 공지용) |
| `bid_ntce_nm` | TEXT | | 공고명 |
| `message` | TEXT | NOT NULL | 알림 본문 |
| `is_read` | INTEGER | DEFAULT 0 | 0: 미읽음, 1: 읽음 |
| `received_at` | INTEGER | NOT NULL | FCM 수신 일시 |

---

## 5. Supabase DB 테이블 스키마 (서버 동기화용)

### `users`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK, FK → auth.users.id | Supabase Auth UID |
| `email` | text | NOT NULL | Google 계정 이메일 |
| `display_name` | text | | 표시 이름 |
| `fcm_token` | text | | 현재 기기 FCM 토큰 (다기기 미지원) |
| `created_at` | timestamptz | DEFAULT now() | 가입일시 |
| `updated_at` | timestamptz | DEFAULT now() | 최종 업데이트 일시 |

RLS: `auth.uid() = id` → 본인 row만 SELECT/UPDATE 허용

---

### `bid_notices`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK, DEFAULT gen_random_uuid() | |
| `user_id` | uuid | FK → users.id, NOT NULL | 소유 사용자 |
| `bid_ntce_no` | text | NOT NULL | 입찰공고번호 |
| `bid_ntce_nm` | text | NOT NULL | 공고명 |
| `ntce_instt_nm` | text | | 공고기관명 |
| `dminstt_nm` | text | | 수요기관명 |
| `bid_ntce_dt` | timestamptz | | 공고일시 |
| `bid_clse_dt` | timestamptz | | 마감일시 |
| `openg_dt` | timestamptz | | 개찰일시 |
| `presmpt_prce` | bigint | | 추정가격 (원) |
| `bdgt_amt` | bigint | | 예산금액 (원) |
| `bid_category` | text | NOT NULL | CNSTWK / SERVC / THNG |
| `current_status` | text | DEFAULT 'REGISTERED' | 현재 공고 상태 |
| `bid_ntce_dtl_url` | text | | 나라장터 상세 URL |
| `last_checked_at` | timestamptz | | GitHub Actions 마지막 폴링 시각 |
| `watched_at` | timestamptz | DEFAULT now() | 관심등록 일시 |

UNIQUE: `(user_id, bid_ntce_no)` — 동일 공고 중복 등록 방지  
Realtime: `current_status` UPDATE 이벤트 구독 활성화  
RLS: 본인 row만 SELECT/INSERT/DELETE. GitHub Actions는 `service_role` 키로 RLS 우회 UPDATE

---

### `bid_notice_status_history`

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | uuid | PK, DEFAULT gen_random_uuid() | |
| `bid_notice_id` | uuid | FK → bid_notices.id | 대상 관심공고 |
| `previous_status` | text | NOT NULL | 변경 전 상태 |
| `new_status` | text | NOT NULL | 변경 후 상태 |
| `ntce_kind_nm` | text | | 조달청 원본 ntceKindNm 값 |
| `detected_at` | timestamptz | DEFAULT now() | 변경 감지 일시 |

RLS: 본인 공고 이력만 SELECT. INSERT는 service_role 전용.

---

## 6. Navigation 구조 (route 정의 전체)

### Bottom Navigation 탭

| 순서 | 탭명 | 라우트 | 아이콘 |
|---|---|---|---|
| 1 | 입찰공고 | `bid/list?category=CNSTWK` | 문서 |
| 2 | 관심공고 | `watchlist` | 하트 |
| 3 | 낙찰결과 | `result` | 트로피 |
| 4 | 알림 | `notifications` | 벨 |
| 5 | 설정 | `settings` | 기어 |

### 전체 라우트 정의

| 라우트 | 화면 | 인수 | Bottom Nav 표시 |
|---|---|---|---|
| `splash` | SplashScreen | 없음 | N |
| `login` | LoginScreen | 없음 | N |
| `bid/list` | BidListScreen | `category: String = "CNSTWK"` (Optional) | Y (탭 1) |
| `bid/search` | SearchScreen | 없음 | N |
| `bid/detail/{bidNtceNo}` | BidDetailScreen | `bidNtceNo: String` | N |
| `watchlist` | WatchlistScreen | 없음 | Y (탭 2) |
| `result` | BidResultListScreen | 없음 | Y (탭 3) |
| `notifications` | NotificationListScreen | 없음 | Y (탭 4) |
| `settings` | SettingsScreen | 없음 | Y (탭 5) |

### 화면 전환 규칙

- `splash` → `login` 또는 `bid/list`: popUpTo("splash") inclusive=true (Back Stack에서 제거)
- `login` → `bid/list`: popUpTo("login") inclusive=true
- `bid/list` → `bid/search`: 일반 navigate (Back으로 돌아오면 검색 결과 반영)
- `bid/list` 또는 `watchlist` → `bid/detail/{bidNtceNo}`: ModalBottomSheet 진입 (NavBackStack 별도 관리)

---

## 7. Android API 레이어 설계

### RetrofitClient + CommonParamInterceptor

**OkHttpClient 구성 (NetworkModule 제공)**

| 컴포넌트 | 역할 |
|---|---|
| `AuthInterceptor` (Application) | ServiceKey + type=json 자동 주입. `addEncodedQueryParameter` 사용 (이중 인코딩 방지) |
| `RetryInterceptor` (Application) | HTTP 429 / 5xx 지수 백오프. 최대 3회 재시도 |
| `HttpLoggingInterceptor` (Network) | DEBUG 빌드 전용. RELEASE에서 제거 |
| ConnectTimeout | 15초 |
| ReadTimeout | 30초 |

**RetrofitClient 설정**

| 항목 | 값 |
|---|---|
| Base URL | `https://apis.data.go.kr/1230000/` |
| Converter | GsonConverterFactory (커스텀 TypeAdapter 포함) |
| CallAdapter | (기본 Call. CoroutineCallAdapterFactory 없이 suspend 함수 사용) |

---

### BidPublicInfoApi Retrofit 인터페이스

공사(CNSTWK) / 용역(SERVC) / 물품(THNG) 각 오퍼레이션:

| 오퍼레이션 | 엔드포인트 | 주요 파라미터 |
|---|---|---|
| 공사공고 목록 | `BidPublicInfoService/getBidPblancListInfoCnstwk` | `inqryDiv`, `inqryBgnDt`, `inqryEndDt`, `bidNtceNm`, `pageNo`, `numOfRows` |
| 용역공고 목록 | `BidPublicInfoService/getBidPblancListInfoServc` | 동일 |
| 물품공고 목록 | `BidPublicInfoService/getBidPblancListInfoThng` | 동일 |
| 공고 상세 (단건) | `BidPublicInfoService/getBidPblancListInfoCnstwk` (inqryDiv=2) | `bidNtceNo`, `bidNtceOrd` |

### ScsbidInfoApi Retrofit 인터페이스

| 오퍼레이션 | 엔드포인트 | 용도 |
|---|---|---|
| 개찰결과 목록 | `ScsbidInfoService/getOpengResultListInfoOpengCompt` | 개찰완료(OPENED) 여부 확인 |
| 낙찰결과 목록 | `ScsbidInfoService/getScsbidListInfoCnstwk` 등 | BidResultListScreen 표시 |

---

### PagingSource 구현 방식

**`BidNoticePagingSource`**

- 생성자 인수: `BidPublicInfoApi`, `SearchParams(category, keyword, dateRange, ...)`
- `load(params)` 처리 흐름:
  1. `pageNo = params.key ?: 1`
  2. `numOfRows = 20` (고정)
  3. API 호출 → `response.body.items` 추출
  4. `totalCount` 기반 다음 페이지 키 결정: `if (pageNo * numOfRows >= totalCount) null else pageNo + 1`
  5. `LoadResult.Page(data, prevKey=null, nextKey)` 반환
  6. 예외 시 `LoadResult.Error(throwable)` 반환
- `PagingConfig(pageSize=20, initialLoadSize=20, prefetchDistance=2)`

---

## 8. 버전 체크 레이어 설계

### GitHub Pages `version.json` 구조

```
{
  "latestVersion": "1.2.0",
  "minRequiredVersion": "1.0.0",
  "downloadUrl": "https://github.com/{owner}/{repo}/releases/download/v1.2.0/app-release.apk",
  "releaseNotes": "버그 수정 및 성능 개선"
}
```

- `latestVersion` > 현재 앱 버전이면 권장 업데이트
- `minRequiredVersion` > 현재 앱 버전이면 강제 업데이트
- 버전 비교: Semantic Versioning (major.minor.patch) 정수 비교

---

### VersionInfo.kt 데이터 클래스 구조

| 필드 | 타입 | 설명 |
|---|---|---|
| `latestVersion` | String | 최신 버전 문자열 |
| `minRequiredVersion` | String | 최소 요구 버전 문자열 |
| `downloadUrl` | String | APK 직접 다운로드 URL |
| `releaseNotes` | String | 업데이트 변경사항 |

---

### VersionCheckRepository 처리 흐름

1. OkHttp로 GitHub Pages `version.json` GET 요청 (`Dispatchers.IO`)
2. 응답 파싱 → `VersionInfo` 객체 생성
3. `BuildConfig.VERSION_NAME`과 버전 비교
4. 결과 반환:
   - `minRequiredVersion > currentVersion` → `FORCE_UPDATE`
   - `latestVersion > currentVersion` → `RECOMMEND_UPDATE`
   - 동일 → `UP_TO_DATE`
   - IOException / 파싱 오류 → `ERROR` (non-blocking, 앱 진행)

---

### SplashViewModel UiState 상태 정의

| 상태 | 전이 조건 | UI 동작 |
|---|---|---|
| `Loading` | 앱 시작 직후 | 로고 + 스피너 |
| `ForceUpdate(downloadUrl, releaseNotes)` | minRequired > current | 취소 불가 다이얼로그 |
| `Downloading(progress: Float)` | ForceUpdate 확인 후 | 선형 프로그레스바 |
| `RecommendUpdate(downloadUrl, releaseNotes)` | latest > current | "지금" / "나중에" 다이얼로그 |
| `UpToDate` | 버전 최신 | 세션 확인 후 화면 분기 |
| `Error(message)` | 네트워크 오류 | 세션 확인 후 화면 분기 (오류 무시) |

---

### APK 다운로드 진행률 관리 방식

- OkHttp `ResponseBody.source()`로 스트리밍 다운로드
- 읽은 바이트 / `contentLength()` 비율을 `MutableStateFlow<Float>`로 방출
- SplashViewModel이 `Downloading(progress)` UiState로 전달
- 다운로드 완료 후 FileProvider URI 생성 → `ACTION_VIEW` Intent

---

### AndroidManifest.xml 추가 항목

| 항목 | 위치 | 내용 |
|---|---|---|
| `INTERNET` | uses-permission | 네트워크 접근 |
| `REQUEST_INSTALL_PACKAGES` | uses-permission | APK 설치 허용 |
| `POST_NOTIFICATIONS` | uses-permission | Android 13+ FCM 알림 |
| `<provider>` | application | FileProvider 선언. `android:authorities="${applicationId}.fileprovider"` |
| `G2bApplication` | application name | @HiltAndroidApp |
| `G2bFirebaseMessagingService` | service | FCM 수신 서비스 |

**`res/xml/file_provider_paths.xml` 설정:**

| 태그 | name | path |
|---|---|---|
| `external-files-path` | `apk_downloads` | `Downloads/` |

---

## 9. GitHub Actions 워크플로우 상세 설계

### `bid-polling.yml` 구조

| 항목 | 값 |
|---|---|
| 트리거 | `schedule: cron: "0 * * * *"` (1시간 간격, Free 플랜 월 744분 소모) |
| 실행 환경 | `ubuntu-latest` / Node.js 20 |
| Steps | checkout → Node.js 설치 → `npm ci` (scripts/) → `node scripts/poll-bid-status.js` |
| Secrets | `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `GOGOV_API_KEY`, `FCM_SERVER_KEY` |
| Timeout | `timeout-minutes: 10` |
| 에러 처리 | `continue-on-error: false` (실패 시 GitHub 알림) |

> **Free 플랜 여유:** 1시간 간격 = 월 744회 × 평균 ~1분 = 월 744분. Free 플랜 2,000분 내 운영 가능.

---

### `poll-bid-status.js` 처리 흐름

**Step 1 — 관심공고 목록 수집**

- `service_role` 키로 Supabase `bid_notices` 전체 조회 (RLS 우회)
- 수집 필드: `id`, `user_id`, `bid_ntce_no`, `bid_category`, `current_status`
- `bid_ntce_no` 기준으로 중복 제거(Deduplication) → 공고 단위 API 호출 최소화
- 상태가 `OPENED` 또는 `CANCELLED`인 공고는 폴링 대상에서 제외 (최종 상태)

**Step 2 — 조달청 API 상태 확인**

- `BidPublicInfoService` GET 호출 (`inqryDiv=2`, `bid_ntce_no` 파라미터)
- `ntceKindNm` 값 → 상태 매핑:
  - "등록" → `REGISTERED`
  - "변경" → `CHANGED`
  - "취소" → `CANCELLED`
  - "재공고" → `REOPENED`
- 별도로 `getOpengResultListInfoOpengCompt` 호출 → 개찰완료 시 `OPENED`
- 요청 간 50ms delay 삽입 (조달청 30 tps 제한 대응)

**Step 3 — 변경 감지 및 DB 업데이트**

- DB `current_status`와 API 응답 상태 비교
- 상태 변경 감지 시:
  1. `bid_notices.current_status`, `last_checked_at` UPDATE → Supabase Realtime 이벤트 자동 발생
  2. `bid_notice_status_history` INSERT
  3. 해당 공고 구독자 전체의 `fcm_token` 조회 (동일 `bid_ntce_no` 구독 사용자 포함)
  4. FCM HTTP v1 API 호출 (아래 참조)
- 상태 변경 없을 시: 해당 공고 `last_checked_at`만 UPDATE

**Step 4 — Supabase 활성 상태 유지 (Ping)**

- 매 실행 종료 시 `bid_notices` 테이블에 경량 SELECT 쿼리 실행 (`limit 1`)
- Supabase Free 플랜 1주일 미사용 시 자동 일시정지 방지

---

### FCM HTTP v1 API 발송 방식

- 엔드포인트: `https://fcm.googleapis.com/v1/projects/{projectId}/messages:send`
- 인증: OAuth2 Bearer Token (Service Account JSON 또는 `FCM_SERVER_KEY` 사용)
- 발송 단위: 기기별 1건씩 개별 POST (배치 전송 미사용, 초기 단계)
- 알림 페이로드:

| 필드 | 값 |
|---|---|
| `notification.title` | 공고명 (최대 50자 말줄임) |
| `notification.body` | "[상태] {ntceKindNm} — {ntce_instt_nm}" |
| `data.bid_ntce_no` | 공고번호 (앱에서 상세 팝업 진입용) |
| `data.new_status` | 변경 후 상태 |

---

### Supabase Ping 쿼리 방식

- `bid_notices` 테이블에 `SELECT id FROM bid_notices LIMIT 1` 실행
- 응답 결과와 무관하게 연결 자체가 ping으로 동작
- 데이터가 0건이어도 Supabase 프로젝트 활성 상태 유지 효과 있음

---

## 10. 각 Phase별 예상 이슈 및 대응 방안

### Phase 1 — 프로젝트 세팅

| 이슈 | 원인 | 대응 |
|---|---|---|
| AGP 9.0.0-alpha06 빌드 오류 | 실험적 API 변경 | 8.9.2로 다운그레이드. 추후 9.x 정식 출시 후 재마이그레이션 |
| Hilt + KSP 충돌 | KSP 버전 불일치 | `ksp` 버전을 Kotlin 버전과 동기화 (`2.1.20-1.0.31`) |
| kotlinx.serialization ↔ Gson 공존 | 직렬화 라이브러리 혼용 | Supabase SDK는 serialization, Retrofit은 Gson으로 역할 분리. 패키지 경계 혼용 금지 |

---

### Phase 2 — 버전 체크

| 이슈 | 원인 | 대응 |
|---|---|---|
| GitHub Pages CORS | 앱에서 직접 호출 시 불필요 (Android는 브라우저 아님) | Android OkHttp는 CORS 제약 없음. 무관 |
| APK 설치 거부 | 출처를 알 수 없는 앱 설치 차단 | 설치 전 `canRequestPackageInstalls()` 확인 → `ACTION_MANAGE_UNKNOWN_APP_SOURCES` Intent로 사용자 안내 |
| APK 다운로드 중 앱 종료 | 프로세스 킬 | WorkManager로 다운로드 작업 이동 검토 (Phase 2 초기에는 ForegroundService로 단순 구현) |
| `getExternalFilesDir` null | 외부 저장소 마운트 해제 | null 체크 후 `filesDir` (내부 저장소) 폴백 |

---

### Phase 3 — 로그인

| 이슈 | 원인 | 대응 |
|---|---|---|
| Credential Manager Play Services 미설치 | 에뮬레이터 또는 구형 기기 | `GetCredentialException` 캐치 → SnackBar 안내 |
| Supabase Google Provider 미설정 | 대시보드 설정 누락 | 구현 전 Supabase 대시보드 → Authentication → Providers → Google 활성화 필수 |
| ID Token 만료(1시간) | 세션 재시작 시 재발급 불필요 | 로그인 후 Supabase 자체 세션(access_token + refresh_token)으로 전환. Google ID Token 재사용 없음 |
| 카카오 로그인 | Kakao SDK 추가 필요 | Phase 3에서는 Google만 구현. Kakao는 Phase 3-B로 분리 (Kakao SDK + Supabase Custom Token 방식) |

---

### Phase 4 — Paging3 + API 연동

| 이슈 | 원인 | 대응 |
|---|---|---|
| ServiceKey 이중 인코딩 | OkHttp `addQueryParameter()` URL 인코딩 | `addEncodedQueryParameter()` 사용. 반드시 사전 검증 |
| `items` 단건/배열 혼용 | 공공 API 공통 패턴 | Gson 커스텀 TypeAdapter 작성. 구현 전 Postman 실제 응답 수집 필수 |
| 429 Too Many Requests | 30 tps 초과 | RetryInterceptor 지수 백오프. PagingConfig prefetchDistance=2로 선행 로딩 최소화 |
| totalCount 0 또는 누락 | API 무응답 또는 필드 누락 | null-safe 처리. 0이면 빈 목록 표시 |

---

### Phase 5 — 상세 팝업

| 이슈 | 원인 | 대응 |
|---|---|---|
| ModalBottomSheet 높이 | Compose 기본값이 화면 절반 | `sheetState = SheetValue.Expanded` 기본값으로 설정. `skipPartiallyExpanded = true` |
| CustomTabs 미설치 기기 | Chrome 등 없는 환경 | `CustomTabsIntent` 실패 시 `ACTION_VIEW` 일반 브라우저 Intent 폴백 |

---

### Phase 6 — 관심공고 저장

| 이슈 | 원인 | 대응 |
|---|---|---|
| Room ↔ Supabase 동기화 불일치 | 네트워크 오류 중 앱 종료 | Room을 단일 소스(SSOT)로 사용. Supabase 동기화 실패 시 Room은 유지하고 재시도 큐잉 |
| 중복 등록 | 네트워크 지연 중 다중 탭 | Room UNIQUE 제약(`bid_ntce_no`) + `INSERT OR IGNORE` 전략 |
| Supabase UNIQUE 위반 | 동일 공고 재등록 | `upsert()` 사용 (ON CONFLICT DO NOTHING) |

---

### Phase 7 — 관심공고 목록

| 이슈 | 원인 | 대응 |
|---|---|---|
| 스와이프 삭제 후 실행취소 | Snackbar 타이밍 이슈 | 삭제를 즉시 Room에서 수행하되, Snackbar "실행취소" 선택 시 재INSERT. Supabase는 최종 확정 후 동기화 |
| 빈 목록 상태 | 관심공고 0건 | EmptyView 컴포넌트로 "관심공고를 등록해보세요" 안내 |

---

### Phase 8 — Push 알림 및 Realtime

| 이슈 | 원인 | 대응 |
|---|---|---|
| GitHub Actions Free 플랜 한도 | 월 2,000분 제한 | 1시간 간격(월 744분) 유지. 초과 시 Option C(cron-job.org) 검토 |
| Realtime WebSocket 연결 끊김 | Android 백그라운드 프로세스 제한 | `DefaultLifecycleObserver`로 포그라운드에서만 구독. 백그라운드는 FCM으로 커버 |
| 다기기 FCM 토큰 충돌 | 로그인 기기 교체 | 로그인 시 최신 토큰으로 Upsert. 구기기 토큰은 자동 무효화 |
| FCM HTTP v1 인증 | OAuth2 토큰 발급 복잡성 | GitHub Actions에서 Service Account JSON을 Secret으로 관리. `google-auth-library` Node.js 패키지 사용 |
| Supabase Realtime 200 연결 제한 | Free 플랜 동시 연결 | 초기 소규모 운영에서는 문제 없음. 사용자 100명 이상 시 Pro 플랜 검토 |
| 관심공고 수 증가 시 API 부하 | 공고 단위 API 호출 선형 증가 | `bid_ntce_no` Deduplication으로 중복 호출 제거. `OPENED`/`CANCELLED` 공고 폴링 제외 |

---

## 부록: 브랜드 색상 정의

| 이름 | 색상코드 | 용도 |
|---|---|---|
| `NavyBlue` | `#001E40` | 앱 아이콘 배경, 타이틀 텍스트, Primary 색상 |
| `TextGray` | `#43474F` | 부제목, 링크 텍스트 |
| `ButtonTextDark` | `#191C1D` | Google 버튼 텍스트 |
| `BorderGray` | `#C3C6D1` | Google 버튼 테두리 |
| `KakaoYellow` | `#FEE500` | 카카오 버튼 배경 |
| `StatusChanged` | `#FF6D00` | 변경 상태 배지 (주황) |
| `StatusCancelled` | `#BA1A1A` | 취소 상태 배지 (빨강) |
| `StatusReopened` | `#0057CC` | 재공고 상태 배지 (파랑) |
| `StatusOpened` | `#6E7680` | 개찰 상태 배지 (회색) |

---

*이 문서를 검토하신 후 승인해주시면 Phase 1부터 순서대로 구현을 시작하겠습니다.*

---

## 11. 프로젝트 전체 구조

> **현재 상태 기준 패키지명:** `com.example.g2b_bid_app` → Phase 1에서 `com.g2b.bidapp`으로 변경 예정  
> **범례:** `(*)` = Phase 1 완료 기준 생성, 나머지는 각 Phase 구현 시 추가

```
g2bbidapp/                                          ← 프로젝트 루트
│
├── .github/
│   └── workflows/
│       └── bid-polling.yml                         ← Phase 8: 조달청 API 폴링 Cron (1시간 간격)
│
├── scripts/                                        ← Phase 8: GitHub Actions에서 실행하는 Node.js 스크립트
│   ├── package.json
│   └── poll-bid-status.js                          ← 관심공고 상태 변경 감지 + FCM 발송
│
├── gradle/
│   ├── libs.versions.toml                          ← Phase 1 (*): Version Catalog (전체 의존성 버전 중앙화)
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── app/
│   ├── google-services.json                        ← Phase 8: Firebase 프로젝트 연동 설정
│   ├── proguard-rules.pro
│   ├── build.gradle.kts                            ← Phase 1 (*): AGP 8.9.2 / KSP / Hilt / Compose 설정
│   │
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml                 ← Phase 1 (*) + Phase 2/8에서 권한·서비스 추가
│       │   │                                            INTERNET, REQUEST_INSTALL_PACKAGES,
│       │   │                                            POST_NOTIFICATIONS, FileProvider,
│       │   │                                            G2bApplication, G2bFirebaseMessagingService
│       │   │
│       │   ├── res/
│       │   │   ├── values/
│       │   │   │   ├── colors.xml
│       │   │   │   ├── strings.xml
│       │   │   │   └── themes.xml
│       │   │   └── xml/
│       │   │       ├── backup_rules.xml
│       │   │       ├── data_extraction_rules.xml
│       │   │       └── file_provider_paths.xml     ← Phase 2: APK 다운로드 경로 (external-files-path)
│       │   │
│       │   └── java/com/g2b/bidapp/
│       │       │
│       │       ├── G2bApplication.kt               ← Phase 1 (*): @HiltAndroidApp
│       │       ├── MainActivity.kt                 ← Phase 1 (*): @AndroidEntryPoint, AppNavGraph 진입점
│       │       │
│       │       ├── di/                             ← Phase 1 (*): Hilt DI 모듈 모음
│       │       │   ├── NetworkModule.kt            ← OkHttpClient + Retrofit 제공
│       │       │   │                                    AuthInterceptor, RetryInterceptor,
│       │       │   │                                    HttpLoggingInterceptor (DEBUG 전용)
│       │       │   ├── DatabaseModule.kt           ← Room G2bDatabase + DAO 제공
│       │       │   ├── RepositoryModule.kt         ← Repository 인터페이스 → Impl 바인딩
│       │       │   └── SupabaseModule.kt           ← SupabaseClient (Ktor Android 엔진) 제공
│       │       │
│       │       ├── navigation/                     ← Phase 1 (*): Navigation 뼈대
│       │       │   ├── Screen.kt                  ← sealed class, 전체 route 문자열 정의
│       │       │   └── AppNavGraph.kt             ← NavHost: splash → login → bid/list 등 전체 구성
│       │       │
│       │       ├── ui/
│       │       │   ├── theme/                     ← Phase 1 (*): Material3 테마
│       │       │   │   ├── Color.kt               ← 브랜드 색상 (NavyBlue, KakaoYellow, StatusChanged 등)
│       │       │   │   ├── Theme.kt               ← G2bBidAppTheme (Light/Dark)
│       │       │   │   └── Type.kt
│       │       │   │
│       │       │   ├── components/                ← 공통 재사용 컴포넌트
│       │       │   │   ├── BidNoticeCard.kt       ← Phase 4: 공고 카드 (공고명·기관·마감일·금액·하트)
│       │       │   │   ├── BidStatusBadge.kt      ← Phase 7: 상태 배지 (5가지 색상)
│       │       │   │   ├── ErrorView.kt           ← Phase 4: 에러 상태 + 재시도 버튼
│       │       │   │   └── EmptyView.kt           ← Phase 4: 빈 목록 안내
│       │       │   │
│       │       │   ├── splash/                    ← Phase 2
│       │       │   │   ├── SplashScreen.kt        ← 로고 + 버전 체크 분기 UI
│       │       │   │   └── SplashViewModel.kt     ← StateFlow<SplashUiState>
│       │       │   │                                    Loading / ForceUpdate / Downloading /
│       │       │   │                                    RecommendUpdate / UpToDate / Error
│       │       │   │
│       │       │   ├── login/                     ← Phase 3
│       │       │   │   ├── LoginScreen.kt         ← Google / Kakao / Guest Mode 버튼
│       │       │   │   └── LoginViewModel.kt      ← StateFlow<LoginUiState>
│       │       │   │
│       │       │   ├── bid/
│       │       │   │   ├── list/                  ← Phase 4
│       │       │   │   │   ├── BidListScreen.kt   ← 탭(공사·용역·물품) + Paging3 LazyColumn
│       │       │   │   │   └── BidListViewModel.kt
│       │       │   │   ├── search/                ← Phase 4
│       │       │   │   │   ├── SearchScreen.kt    ← 키워드·날짜범위·업종 필터
│       │       │   │   │   └── SearchViewModel.kt
│       │       │   │   └── detail/                ← Phase 5
│       │       │   │       ├── BidDetailScreen.kt ← ModalBottomSheet 전체 크기 팝업
│       │       │   │       └── BidDetailViewModel.kt
│       │       │   │
│       │       │   ├── watchlist/                 ← Phase 7
│       │       │   │   ├── WatchlistScreen.kt     ← SwipeToDismissBox + BidStatusBadge
│       │       │   │   └── WatchlistViewModel.kt
│       │       │   │
│       │       │   ├── notification/              ← Phase 8
│       │       │   │   ├── NotificationListScreen.kt
│       │       │   │   └── NotificationListViewModel.kt
│       │       │   │
│       │       │   └── settings/                  ← Phase 8
│       │       │       └── SettingsScreen.kt      ← 알림 유형별 ON/OFF + 로그아웃
│       │       │
│       │       ├── domain/                        ← 순수 Kotlin, Android 의존성 없음
│       │       │   ├── model/                     ← Domain 모델 (DTO·Entity와 분리)
│       │       │   │   ├── BidNotice.kt           ← 입찰공고 도메인 모델
│       │       │   │   ├── WatchedBid.kt          ← 관심공고 도메인 모델
│       │       │   │   ├── BidStatus.kt           ← enum: REGISTERED/CHANGED/CANCELLED/REOPENED/OPENED
│       │       │   │   ├── BidCategory.kt         ← enum: CNSTWK/SERVC/THNG
│       │       │   │   ├── SearchParams.kt        ← 검색 파라미터 묶음 (keyword, dateRange, category)
│       │       │   │   ├── VersionInfo.kt         ← 버전 체크 결과 (latestVersion, minRequired, downloadUrl)
│       │       │   │   └── NotificationItem.kt    ← 알림 도메인 모델
│       │       │   │
│       │       │   ├── repository/                ← Repository 인터페이스 (Data Layer 역방향 의존성 역전)
│       │       │   │   ├── BidRepository.kt
│       │       │   │   ├── WatchlistRepository.kt
│       │       │   │   └── AuthRepository.kt
│       │       │   │
│       │       │   └── usecase/
│       │       │       ├── GetBidNoticeListUseCase.kt         ← Phase 4: SearchParams → Flow<PagingData<BidNotice>>
│       │       │       ├── GetWatchlistUseCase.kt             ← Phase 6: Room Flow 구독
│       │       │       ├── AddToWatchlistUseCase.kt           ← Phase 6: 중복 체크 → Room+Supabase INSERT
│       │       │       ├── RemoveFromWatchlistUseCase.kt      ← Phase 6: Room+Supabase DELETE
│       │       │       ├── ObserveWatchlistRealtimeUseCase.kt ← Phase 8: Realtime 이벤트 → Room UPDATE
│       │       │       └── SignInWithGoogleUseCase.kt         ← Phase 3: Credential Manager → ID Token
│       │       │
│       │       └── data/
│       │           ├── local/                     ← Room DB
│       │           │   ├── G2bDatabase.kt         ← Phase 6: RoomDatabase (watched_bids, bid_status_history, notifications)
│       │           │   ├── dao/
│       │           │   │   ├── WatchedBidDao.kt           ← Flow<List<WatchedBidEntity>>, INSERT OR IGNORE, DELETE
│       │           │   │   ├── BidStatusHistoryDao.kt     ← INSERT, 공고별 이력 조회
│       │           │   │   └── NotificationDao.kt         ← INSERT, 읽음 처리 UPDATE
│       │           │   └── entity/
│       │           │       ├── WatchedBidEntity.kt        ← watched_bids 테이블 매핑
│       │           │       ├── BidStatusHistoryEntity.kt  ← bid_status_history 테이블 매핑
│       │           │       └── NotificationEntity.kt      ← notifications 테이블 매핑
│       │           │
│       │           ├── remote/
│       │           │   ├── api/                   ← Retrofit 인터페이스
│       │           │   │   ├── BidPublicInfoApi.kt  ← Phase 4: 공사·용역·물품 공고 목록/상세 (조달청)
│       │           │   │   └── ScsbidInfoApi.kt     ← Phase 4/8: 개찰결과·낙찰결과 조회
│       │           │   ├── dto/
│       │           │   │   ├── BidNoticeDto.kt              ← 조달청 API 응답 필드 1:1 매핑
│       │           │   │   ├── BidNoticeListResponse.kt     ← 페이징 응답 래퍼 (totalCount, items)
│       │           │   │   └── BidItemsTypeAdapter.kt       ← Gson: items 단건 객체/배열 혼용 대응
│       │           │   ├── interceptor/
│       │           │   │   ├── AuthInterceptor.kt   ← ServiceKey + type=json 자동 주입 (addEncodedQueryParameter)
│       │           │   │   └── RetryInterceptor.kt  ← HTTP 429/5xx 지수 백오프 (최대 3회)
│       │           │   └── paging/
│       │           │       └── BidNoticePagingSource.kt  ← Phase 4: pageNo/numOfRows(20)/totalCount 관리
│       │           │
│       │           ├── mapper/
│       │           │   ├── BidNoticeMapper.kt       ← BidNoticeDto → BidNotice (Domain)
│       │           │   └── WatchedBidMapper.kt      ← WatchedBidEntity → WatchedBid (Domain)
│       │           │
│       │           ├── repository/                  ← Repository 구현체
│       │           │   ├── BidRepositoryImpl.kt     ← Phase 4: PagingSource + Mapper 조합
│       │           │   ├── WatchlistRepositoryImpl.kt ← Phase 6: Room SSOT + Supabase 동기화
│       │           │   └── AuthRepositoryImpl.kt    ← Phase 3: Credential Manager + Supabase Auth
│       │           │
│       │           ├── version/                     ← Phase 2: 버전 체크 레이어
│       │           │   └── VersionCheckRepository.kt  ← OkHttp GET → VersionInfo 파싱 → FORCE/RECOMMEND/UP_TO_DATE
│       │           │
│       │           └── service/
│       │               ├── G2bFirebaseMessagingService.kt   ← Phase 8: FCM 수신 → NotificationEntity INSERT + 알림 표시
│       │               └── SupabaseRealtimeService.kt       ← Phase 8: WebSocket 구독 (포그라운드) → Room UPDATE
│       │
│       ├── test/
│       │   └── java/com/g2b/bidapp/
│       │       └── ExampleUnitTest.kt
│       │
│       └── androidTest/
│           └── java/com/g2b/bidapp/
│               └── ExampleInstrumentedTest.kt
│
├── build.gradle.kts                                ← Phase 1 (*): AGP 8.9.2, Kotlin 2.1.20 플러그인 선언
├── settings.gradle.kts
├── gradle.properties
├── local.properties                                ← GOGOV_API_KEY 등 민감 키 (Git 제외)
├── plan.md
└── research.md
```

---

### 레이어 의존 방향 요약

```
[UI Layer]  →  [Domain Layer]  →  [Data Layer]
 ui/             domain/             data/
 (Compose)       (model,             (local, remote,
                  repository,         mapper, repository/,
                  usecase)            version, service)
```

- **UI → Domain**: ViewModel이 UseCase를 직접 주입받아 호출
- **Domain → Data**: Repository 인터페이스(Domain)를 Impl(Data)이 구현 (DIP 적용)
- **Domain ↔ Data 격리**: DTO·Entity는 Domain 모델로 Mapper를 통해 변환, Domain 패키지에는 Android 의존성 없음

---

### 핵심 외부 의존성 요약

| 역할 | 라이브러리 |
|---|---|
| UI | Jetpack Compose + Material3 |
| DI | Hilt (KSP) |
| 네비게이션 | Navigation Compose |
| 네트워크 | Retrofit2 + OkHttp3 + Gson |
| 페이징 | Paging3 |
| 로컬 DB | Room (KSP) |
| 백엔드 동기화 | Supabase-kt (postgrest, auth, realtime) |
| 인증 | Credential Manager (Google ID Token) |
| Push 알림 | Firebase Cloud Messaging (FCM) |
| 코루틴 | kotlinx.coroutines |

---

## 12. 테스트 전략

### 테스트 라이브러리 (`libs.versions.toml` 추가 항목)

| 라이브러리 | 용도 | 적용 위치 |
|---|---|---|
| `junit:junit:4.13.2` | 단위 테스트 러너 | `testImplementation` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `TestCoroutineScheduler`, `runTest`, `UnconfinedTestDispatcher` | `testImplementation` |
| `io.mockk:mockk` | Kotlin 친화적 Mocking (suspend 함수 지원) | `testImplementation` |
| `app.cash.turbine:turbine` | `Flow` / `StateFlow` 방출값 순서 검증 | `testImplementation` |
| `com.squareup.okhttp3:mockwebserver` | HTTP 응답 시뮬레이션 (RetryInterceptor, VersionCheckRepository) | `testImplementation` |
| `androidx.arch.core:core-testing` | `InstantTaskExecutorRule` (LiveData 동기 실행) | `testImplementation` |
| `androidx.room:room-testing` | `in-memory` RoomDatabase 생성 헬퍼 | `androidTestImplementation` |
| `androidx.test.ext:junit` | `AndroidJUnit4` 러너 | `androidTestImplementation` |
| `androidx.compose.ui:ui-test-junit4` | `createAndroidComposeRule`, Compose 노드 탐색 | `androidTestImplementation` |
| `com.google.dagger:hilt-android-testing` | `@HiltAndroidTest`, `HiltAndroidRule` | `androidTestImplementation` |
| `androidx.paging:paging-testing` | `TestPager`, `LoadResult` 검증 | `testImplementation` |

---

### 테스트 파일 구조

```
app/src/
│
├── test/java/com/g2b/bidapp/           ← JVM Unit Tests (Android 프레임워크 불필요)
│   │
│   ├── domain/usecase/
│   │   ├── GetBidNoticeListUseCaseTest.kt
│   │   ├── AddToWatchlistUseCaseTest.kt
│   │   ├── RemoveFromWatchlistUseCaseTest.kt
│   │   └── GetWatchlistUseCaseTest.kt
│   │
│   ├── data/mapper/
│   │   ├── BidNoticeMapperTest.kt
│   │   └── WatchedBidMapperTest.kt
│   │
│   ├── data/remote/
│   │   ├── interceptor/
│   │   │   ├── AuthInterceptorTest.kt
│   │   │   └── RetryInterceptorTest.kt
│   │   ├── paging/
│   │   │   └── BidNoticePagingSourceTest.kt
│   │   └── dto/
│   │       └── BidItemsTypeAdapterTest.kt
│   │
│   ├── data/version/
│   │   └── VersionCheckRepositoryTest.kt
│   │
│   └── ui/
│       ├── SplashViewModelTest.kt
│       ├── LoginViewModelTest.kt
│       ├── BidListViewModelTest.kt
│       ├── WatchlistViewModelTest.kt
│       └── BidDetailViewModelTest.kt
│
└── androidTest/java/com/g2b/bidapp/    ← Instrumented Tests (기기/에뮬레이터 필요)
    │
    ├── data/local/dao/
    │   ├── WatchedBidDaoTest.kt
    │   ├── BidStatusHistoryDaoTest.kt
    │   └── NotificationDaoTest.kt
    │
    ├── data/repository/
    │   └── WatchlistRepositoryImplTest.kt
    │
    └── ui/
        ├── SplashScreenTest.kt
        ├── LoginScreenTest.kt
        ├── BidListScreenTest.kt
        └── WatchlistScreenTest.kt
```

---

### 레이어별 테스트 전략

#### Domain Layer — UseCase 단위 테스트

- **의존성 처리**: Repository 인터페이스를 MockK로 mock하거나 `Fake` 구현체 작성
- **코루틴**: `runTest` + `UnconfinedTestDispatcher` 사용
- **Flow 검증**: Turbine의 `test { }` 블록으로 방출 순서 단언

```kotlin
// 예시: AddToWatchlistUseCaseTest
@Test
fun `이미 등록된 공고는 중복 등록하지 않는다`() = runTest {
    val fakeRepo = FakeWatchlistRepository(existingBids = listOf(sampleBid))
    val useCase = AddToWatchlistUseCase(fakeRepo)
    val result = useCase(sampleBid)
    assertThat(result).isInstanceOf(Result.Failure::class.java)
    assertThat(fakeRepo.insertCallCount).isEqualTo(0)
}
```

---

#### Data Layer — Mapper 단위 테스트

- Android 의존성 없는 순수 Kotlin 변환 함수이므로 JUnit4 + AssertJ로 충분
- **중점 검증 항목**:
  - `presmptPrce` null → Domain 모델 기본값(0) 처리
  - 날짜 문자열 → Unix Timestamp 변환 정확성
  - `bid_category` 문자열 → `BidCategory` enum 매핑 누락 처리

---

#### Data Layer — RetryInterceptor 단위 테스트 (MockWebServer)

| 시나리오 | 서버 응답 설정 | 기댓값 |
|---|---|---|
| 첫 번째 요청 성공 | 200 | 재시도 없이 응답 반환 |
| 429 → 200 | 429 → 200 | 1회 재시도 후 성공 |
| 5xx 3회 연속 | 500 × 3 | 3회 소진 후 `IOException` |
| 4xx (클라이언트 오류) | 400 | 재시도 없이 즉시 실패 반환 |

---

#### Data Layer — VersionCheckRepository 단위 테스트 (MockWebServer)

| 시나리오 | JSON 응답 | 기댓값 |
|---|---|---|
| 현재 버전 = 최신 | `latestVersion == currentVersion` | `UP_TO_DATE` |
| 최신 버전 존재 | `latestVersion > currentVersion` | `RECOMMEND_UPDATE` |
| 강제 업데이트 필요 | `minRequiredVersion > currentVersion` | `FORCE_UPDATE` |
| 네트워크 오류 | 연결 거부 | `ERROR` (non-blocking) |
| 잘못된 JSON | 파싱 불가 응답 | `ERROR` (non-blocking) |

---

#### Data Layer — BidNoticePagingSource 단위 테스트

- `androidx.paging:paging-testing`의 `TestPager` 사용
- **검증 항목**:
  - `LoadResult.Page` 반환 시 `nextKey` 계산 정확성 (`totalCount` / `numOfRows` 기반)
  - 마지막 페이지에서 `nextKey = null` 반환
  - API 예외 시 `LoadResult.Error` 반환
  - `items` 필드 단건/배열 혼용 응답 모두 정상 파싱

---

#### Data Layer — BidItemsTypeAdapter 단위 테스트

| 입력 JSON | 기댓값 |
|---|---|
| `"items": { ... }` (단건 객체) | `List` 1개 요소로 파싱 |
| `"items": [ {...}, {...} ]` (배열) | `List` 다수 요소로 파싱 |
| `"items": null` | 빈 `List` 반환 |

---

#### Data Layer — Room DAO 통합 테스트 (androidTest)

- `Room.inMemoryDatabaseBuilder()` 사용 → 실제 SQLite 실행, 테스트 후 자동 소멸
- **WatchedBidDaoTest 핵심 시나리오**:

| 시나리오 | 검증 항목 |
|---|---|
| INSERT → SELECT | `Flow` 방출값에 삽입된 항목 포함 확인 |
| 중복 INSERT | `INSERT OR IGNORE` → 행 수 유지 |
| DELETE → SELECT | `Flow`에서 삭제된 항목 제거 확인 |
| 키워드 필터 쿼리 | `LIKE '%keyword%'` 결과 일치 확인 |
| `synced_at` UPDATE | 특정 행만 갱신되고 나머지 행 불변 확인 |

---

#### UI Layer — ViewModel 단위 테스트

- `TestCoroutineScheduler` + `StandardTestDispatcher`로 코루틴 실행 타이밍 제어
- Repository를 MockK로 mock하여 ViewModel 단독 실행
- Turbine으로 `StateFlow` 상태 전이 순서 검증

| ViewModel | 핵심 테스트 시나리오 |
|---|---|
| `SplashViewModel` | Loading → ForceUpdate / RecommendUpdate / UpToDate / Error 전이 순서 |
| `SplashViewModel` | Downloading: 진행률 0.0 → 1.0 방출 순서 |
| `LoginViewModel` | Google 로그인 성공 → Success 전이 / 실패 → Error(message) 전이 |
| `BidListViewModel` | 검색 파라미터 변경 시 PagingData 갱신 트리거 |
| `WatchlistViewModel` | 관심공고 삭제 후 Snackbar 실행취소 → Room 재INSERT 호출 확인 |
| `BidDetailViewModel` | 관심공고 토글 → 등록/해제 상태 반전 |

---

#### UI Layer — Compose UI 테스트 (androidTest)

- `createAndroidComposeRule<MainActivity>()` 또는 `createComposeRule()`로 독립 실행
- Hilt 주입이 필요한 경우 `@HiltAndroidTest` + `HiltAndroidRule` 사용

| 화면 | 핵심 시나리오 |
|---|---|
| `SplashScreen` | ForceUpdate 다이얼로그 표시 → "업데이트" 버튼 클릭 가능, "취소" 버튼 없음 확인 |
| `LoginScreen` | Google 로그인 버튼 존재 확인 / Loading 중 버튼 비활성화 확인 |
| `BidListScreen` | 공사·용역·물품 탭 전환 시 LazyColumn 재렌더링 확인 |
| `BidListScreen` | 에러 상태에서 "재시도" 버튼 클릭 → 로딩 상태 전이 확인 |
| `WatchlistScreen` | 항목 스와이프 → 삭제 확인 Snackbar 표시 확인 |
| `BidStatusBadge` | CHANGED(주황) / CANCELLED(빨강) / REOPENED(파랑) 색상 렌더링 |

---

### Phase별 테스트 완료 기준

| Phase | 필수 통과 테스트 |
|---|---|
| Phase 1 | (테스트 인프라 설정 확인) `libs.versions.toml` 테스트 의존성 추가, `ExampleUnitTest` 정상 통과 |
| Phase 2 | `VersionCheckRepositoryTest` 5개 시나리오 전체 통과 / `SplashViewModelTest` 상태 전이 전체 통과 |
| Phase 3 | `LoginViewModelTest` 성공·실패 전이 통과 |
| Phase 4 | `BidNoticePagingSourceTest` 페이지 키 계산·에러 처리 통과 / `BidItemsTypeAdapterTest` 단건·배열·null 통과 / `RetryInterceptorTest` 4개 시나리오 통과 / `BidNoticeMapperTest` null 필드 처리 통과 |
| Phase 5 | `BidDetailViewModelTest` 관심공고 토글 상태 반전 통과 |
| Phase 6 | `WatchedBidDaoTest` 전체 시나리오 통과 / `AddToWatchlistUseCaseTest` 중복 방지 통과 |
| Phase 7 | `WatchlistViewModelTest` 스와이프 삭제·실행취소 통과 / `WatchlistScreenTest` Snackbar 표시 통과 |
| Phase 8 | `NotificationDaoTest` 읽음 처리 통과 |

---

### 테스트 원칙

1. **Mock vs Fake**: Repository 인터페이스는 `FakeXxxRepository` 구현체 우선 작성. 동작이 복잡하지 않은 단순 반환 값은 MockK `every { }` 사용.
2. **Room은 in-memory 실제 DB 사용**: mock DAO 금지. 마이그레이션 오류를 사전 발견하기 위해 실제 SQLite 실행 유지.
3. **Supabase·FCM·Credential Manager는 테스트 대상 제외**: 외부 서비스 경계는 Repository 인터페이스에서 차단하고 Fake로 대체. 실제 네트워크 호출 없음.
4. **Dispatcher 명시**: 모든 ViewModel·UseCase 테스트는 생성자에서 `CoroutineDispatcher`를 주입받는 구조로 작성하여 `UnconfinedTestDispatcher`로 교체 가능하게 유지.
5. **한 테스트 함수 = 한 시나리오**: Given-When-Then 구조. 함수명은 백틱 한글로 의도를 명시 (`\`조건_일때_기댓값\``).
6. **GitHub Actions CI 연동**: `bid-polling.yml`과 별도로 `ci.yml` 워크플로우를 추가하여 PR 생성 시 `./gradlew test` + `./gradlew connectedAndroidTest` 자동 실행.