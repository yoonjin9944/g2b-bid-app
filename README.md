# 나라장터 모니터링

SI개발업체 영업팀을 위한 조달청 나라장터 모니터링 Android 앱

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Hilt](https://img.shields.io/badge/Hilt-34A853?style=flat&logo=google&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-3ECF8E?style=flat&logo=supabase&logoColor=white)
![FCM](https://img.shields.io/badge/FCM-FFCA28?style=flat&logo=firebase&logoColor=black)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat&logo=githubactions&logoColor=white)

---

## 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [주요 기능](#주요-기능)
3. [기술 스택](#기술-스택)
4. [아키텍처](#아키텍처)
5. [화면 흐름](#화면-흐름)
6. [Push 알림](#push-알림)
7. [앱 업데이트](#앱-업데이트)
8. [환경 변수 설정](#환경-변수-설정)
9. [시작하기](#시작하기)

---

## 프로젝트 개요

조달청 나라장터 OpenAPI를 활용하여 입찰공고를 검색하고, 관심 공고의 상태 변경 시 Push 알림을 수신할 수 있는 영업팀 전용 Android 앱입니다. 스마트 나라장터 앱을 벤치마킹하여 설계되었습니다.

---

## 주요 기능

- 입찰공고 통합검색 및 상세검색 (공사 / 용역 / 물품)
- 입찰공고 목록 무한 스크롤
- 공고 상세 정보 전체화면 팝업
- 관심공고 등록 / 삭제
- 관심공고 상태 변경 시 Push 알림
- Google 소셜 로그인
- 앱 버전 자동 체크 및 업데이트

---

## 기술 스택

| 분류 | 기술 | 용도 |
|---|---|---|
| Language | Kotlin | 전체 앱 개발 |
| UI | Jetpack Compose | 선언형 UI |
| Architecture | MVVM + Clean Architecture | 레이어 분리 |
| DI | Hilt | 의존성 주입 |
| Network | Retrofit2 + OkHttp3 | API 통신 |
| Paging | Paging3 | 목록 페이지네이션 |
| Local DB | Room | 관심공고 로컬 캐싱 |
| Backend | Supabase | Auth / DB / Realtime |
| Push | FCM | Push 알림 |
| Scheduler | GitHub Actions | 공고 상태 주기적 확인 |
| 이미지 | Coil | 이미지 로딩 |
| 최소 지원 | Android 8.0 (API 26) | - |

---

## 아키텍처

```
app/
├── data/
│   ├── api/          ← Retrofit interface
│   ├── db/           ← Room Database
│   ├── repository/   ← Repository 구현체
│   └── model/        ← DTO
├── domain/
│   ├── usecase/
│   ├── repository/   ← Repository interface
│   └── model/        ← Domain Model
├── presentation/
│   ├── ui/
│   │   ├── splash/
│   │   ├── login/
│   │   ├── main/
│   │   ├── search/
│   │   ├── detail/
│   │   └── bookmark/
│   └── viewmodel/
├── di/
└── util/
```

---

## 화면 흐름

### 입찰공고 검색

```
Splash → 로그인 → 메인 → 검색 → 목록 → 상세 팝업 → 관심공고 등록
```

### 관심공고 조회

```
메인 → 관심공고 탭 → 목록 → 상세 팝업 또는 삭제
```

---

## Push 알림

GitHub Actions가 주기적으로 관심공고 상태를 확인하여 변경 발생 시 FCM Push를 발송합니다.
앱은 Supabase Realtime을 구독하여 UI를 실시간으로 갱신합니다.

```
GitHub Actions (1시간 주기)
  └── 관심공고 상태 확인
        ├── 변경 감지 → DB 업데이트 → FCM Push 발송
        └── 미변경   → 유지
```

---

## 앱 업데이트

Splash 화면에서 원격 JSON을 기반으로 버전을 확인하고 업데이트 여부를 안내합니다.

| 조건 | 동작 |
|---|---|
| 최소 버전 미만 | 강제 업데이트 (거절 시 앱 종료) |
| 최신 버전 미만 | 권장 업데이트 (나중에 스킵 가능) |
| 최신 버전 이상 또는 오류 | 메인화면으로 이동 |

---

## 환경 변수 설정

### local.properties

```properties
GOGOV_API_KEY=공공데이터포털_발급_ServiceKey
SUPABASE_URL=https://xxxx.supabase.co
SUPABASE_ANON_KEY=your_anon_key
VERSION_CHECK_URL=https://your-pages-url/version.json
```

### GitHub Actions Secrets

```
SUPABASE_URL
SUPABASE_SERVICE_ROLE_KEY
GOGOV_API_KEY
FCM_SERVER_KEY
```

---

## 시작하기

### 1. 저장소 클론

```bash
git clone https://github.com/your-org/bid-monitor-android.git
```

### 2. 사전 준비

- 공공데이터포털에서 나라장터 API 키 발급
- Supabase 프로젝트 생성 및 Google OAuth 설정
- GitHub Actions Secrets 등록
- GitHub Pages에 version.json 배포

### 3. 빌드

```bash
./gradlew assembleDebug
```
