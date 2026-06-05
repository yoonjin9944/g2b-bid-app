# FCM 알림 미수신 원인 분석

> **알림 파이프라인**: GitHub Actions `scripts/poll-bid-status.js` (매시간 정각 실행)
> → Supabase `notifications` 테이블 적재 + FCM v1 API 직접 호출
> → Android `G2bFirebaseMessagingService.onMessageReceived()`

---

## 원인 1 (결정적): FCM 오류를 완전히 삼켜버림 → 실패 원인 파악 불가

`sendPush()` 함수의 catch 블록:

```javascript
} catch (e) {
  const status = e.response?.data?.error?.status;
  if (status === "INVALID_ARGUMENT" || status === "UNREGISTERED") {
    await supabase.from("user_fcm_tokens").delete().eq("fcm_token", fcm_token);
  }
  // console.error 없음 — 다른 모든 오류가 조용히 사라짐
}
```

`INVALID_ARGUMENT`, `UNREGISTERED` 외의 모든 FCM 오류(401 인증 실패, 403 권한 없음, 500 서버 오류 등)가 **로그 없이 무시**된다.
GitHub Actions 로그만 봐서는 FCM 전송이 성공했는지 실패했는지 알 수 없다.

`notifications` 테이블에 데이터가 있어도 FCM 전송은 그 직후 별도로 실행되므로, 테이블 적재 성공 ≠ FCM 발송 성공.

**해결**: catch 블록에 `console.error` 추가 → GitHub Actions 로그로 실제 오류 확인

```javascript
} catch (e) {
  console.error("FCM 전송 실패:", fcm_token.slice(0, 20), e.response?.data ?? e.message);
  const status = e.response?.data?.error?.status;
  if (status === "INVALID_ARGUMENT" || status === "UNREGISTERED") {
    await supabase.from("user_fcm_tokens").delete().eq("fcm_token", fcm_token);
  }
}
```

---

## 원인 2: FIREBASE_SERVICE_ACCOUNT_JSON 시크릿 미설정 or 오류

`getFcmAccessToken()`이 실패하면 `fcmToken`이 `{ token: null }` 형태가 되어 FCM 헤더가 `Bearer null`이 된다.
하지만 `!fcmToken` 체크는 객체 자체가 null일 때만 걸리므로 **`fcmToken.token`이 null이어도 통과**하고 FCM 호출에서 401이 난다 → 위 원인 1에 의해 조용히 삼켜짐.

**확인**: GitHub Actions → Secrets에 `FIREBASE_SERVICE_ACCOUNT_JSON`이 올바른 서비스 계정 JSON으로 등록되어 있는지 확인.

---

## 원인 3 (Android): POST_NOTIFICATIONS 런타임 권한 미요청

Android 13+ (API 33)에서는 `Manifest.xml` 선언만으로 부족하고 **앱 실행 중 사용자에게 명시적으로 권한 요청**해야 한다.
현재 `MainActivity`나 `SplashScreen` 어디에도 이 요청 코드가 없다.

→ `notificationManager.notify()`를 호출해도 권한이 없으면 알림이 표시되지 않음.

**빠른 확인**: 기기 설정 → 앱 → G2B앱 → 알림 → 허용 여부 확인. 꺼져 있으면 이게 원인.

**해결**: `MainActivity`에서 Compose Permission API로 요청 추가:
```kotlin
val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
LaunchedEffect(Unit) { permissionState.launchPermissionRequest() }
```

---

## 우선순위별 대응

1. **즉시 (원인 1)**: `sendPush()` catch 블록에 `console.error` 추가 → 재실행해서 로그 확인
2. **즉시 (원인 3)**: 기기 설정에서 알림 권한 수동으로 켜서 테스트 → Android 코드에 런타임 권한 요청 추가
3. **확인 (원인 2)**: GitHub Actions Secrets에서 `FIREBASE_SERVICE_ACCOUNT_JSON` 값 확인

---

# D-1 관심공고 push 미발송 원인 분석

## 원인 1 (결정적): 날짜 기반 알림 코드가 main에 없음

GitHub Actions workflow는 `actions/checkout@v4` 기본 동작으로 **main 브랜치**를 체크아웃.
날짜 기반 알림(마감 임박 / 투찰 마감 / 개찰) 코드는 **dev 브랜치에만** 존재.
→ 어제/오늘 실행된 workflow는 날짜 기반 알림이 없는 구버전 스크립트를 실행

**해결**: dev → main 머지 필요

---

## 원인 2: 1시간 window 설계 한계

현재 "마감 임박" 조건:
```
23h < (bid_clse_dt - 현재) ≤ 24h
```
→ 폴링이 정각에 실행되므로 이 1시간 구간에서 정각이 한 번이라도 찍혀야만 발송.

D-1에 등록해도 등록 시점과 마감 시각 조합에 따라 window를 놓칠 수 있음:

| 등록 시점 | 마감 시각 | remaining | 결과 |
|---|---|---|---|
| 어제 09:00 | 오늘 10:00 | 25h | poll 10:00에 24h → ✅ 발송 |
| 어제 11:00 | 오늘 10:00 | 23h | 이미 23h 이하 → ❌ window 놓침 |
| 어제 09:30 | 오늘 10:00 | 24.5h | poll 10:00에 24h → ✅ / poll 11:00에 23h → window 이탈 |

→ 마감 24h 이전에 공고를 등록하지 않으면 "마감 임박" 알림을 영구적으로 놓침

**해결**: window를 24h ± 30분으로 확장하거나, 공고 등록 시 이미 24h 이내면 즉시 알림 발송 로직 추가

---

## 원인 3: bid_clse_dt null 가능성

이전 FormatUtils 수정 이전에 등록된 공고는 날짜 변환 실패로 bid_clse_dt = null.
```javascript
const clseDtMs = bid.bid_clse_dt ? new Date(bid.bid_clse_dt).getTime() : null;
// clseDtMs = null → 날짜 기반 알림 블록 전부 skip
```
→ Supabase에서 해당 공고의 bid_clse_dt 컬럼이 null인지 확인 필요

**해결**: 공고를 삭제 후 재등록하면 수정된 변환 로직으로 날짜가 정상 저장됨

---

## 우선순위별 대응

1. **즉시**: dev → main 머지 (원인 1 해결)
2. **단기**: window를 24h ± 30분으로 확장 (원인 2 완화)
3. **확인**: Supabase에서 bid_clse_dt 값 확인 후 null이면 재등록 (원인 3)
