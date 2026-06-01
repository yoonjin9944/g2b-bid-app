const { createClient } = require("@supabase/supabase-js");
const axios = require("axios");
const { GoogleAuth } = require("google-auth-library");

// DB 쿼리 전용 스크립트 — Realtime 미사용
// transport에 stub을 전달해 RealtimeClient 초기화 시 WebSocket 체크를 우회
class NoopWebSocket {
  constructor() { this.readyState = 3; } // CLOSED
  send() {} close() {} addEventListener() {} removeEventListener() {}
}
NoopWebSocket.CONNECTING = 0;
NoopWebSocket.OPEN = 1;
NoopWebSocket.CLOSING = 2;
NoopWebSocket.CLOSED = 3;

const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY,
  { realtime: { transport: NoopWebSocket } }
);

async function getFcmAccessToken() {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
  const auth = new GoogleAuth({
    credentials: serviceAccount,
    scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
  });
  return (await auth.getClient()).getAccessToken();
}

async function main() {
  // 1. 폴링 대상 관심공고 조회 (최종 상태 제외)
  const { data: bids } = await supabase
    .from("bid_notices")
    .select("id, user_id, bid_ntce_no, bid_category, current_status")
    .not("current_status", "in", '("OPENED","CANCELLED")');

  // 2. bid_ntce_no 중복 제거
  const unique = [...new Map(bids.map((b) => [b.bid_ntce_no, b])).values()];

  const fcmToken = await getFcmAccessToken();

  for (const bid of unique) {
    await new Promise((r) => setTimeout(r, 50)); // 30 tps 제한 대응

    // 3. 조달청 API로 현재 상태 확인
    const apiStatus = await fetchBidStatus(bid.bid_ntce_no, bid.bid_category);
    if (!apiStatus) continue;

    // 4. 상태 변경 감지
    const subscribers = bids.filter((b) => b.bid_ntce_no === bid.bid_ntce_no);
    for (const sub of subscribers) {
      if (sub.current_status !== apiStatus) {
        await updateStatus(sub.id, sub.user_id, apiStatus, sub.current_status, bid.bid_ntce_no, fcmToken);
      } else {
        // 변경 없어도 last_checked_at 갱신
        await supabase.from("bid_notices").update({ last_checked_at: new Date().toISOString() }).eq("id", sub.id);
      }
    }
  }

  // 5. Supabase Ping (Free 플랜 자동 일시정지 방지)
  await supabase.from("bid_notices").select("id").limit(1);

  console.log("Polling complete.");
}

async function fetchBidStatus(bidNtceNo, category) {
  const endpoints = {
    CNSTWK: "getBidPblancListInfoCnstwk",
    SERVC: "getBidPblancListInfoServc",
    THNG: "getBidPblancListInfoThng",
  };
  const endpoint = endpoints[category] || "getBidPblancListInfoCnstwk";
  try {
    const res = await axios.get(
      `https://apis.data.go.kr/1230000/ad/BidPublicInfoService/${endpoint}`,
      {
        params: {
          serviceKey: process.env.GOGOV_API_KEY,
          type: "json",
          inqryDiv: 2,
          bidNtceNo,
        },
      }
    );
    const ntceKindNm = res.data?.response?.body?.items?.[0]?.ntceKindNm;
    return mapNtceKindNm(ntceKindNm);
  } catch {
    return null;
  }
}

function mapNtceKindNm(ntceKindNm) {
  if (!ntceKindNm) return null;
  if (ntceKindNm.includes("등록")) return "REGISTERED";
  if (ntceKindNm.includes("변경")) return "CHANGED";
  if (ntceKindNm.includes("취소")) return "CANCELLED";
  if (ntceKindNm.includes("재공고")) return "REOPENED";
  return null;
}

async function updateStatus(bidId, userId, newStatus, prevStatus, bidNtceNo, fcmToken) {
  // DB 업데이트 (Realtime 이벤트 자동 발생)
  await supabase
    .from("bid_notices")
    .update({ current_status: newStatus, last_checked_at: new Date().toISOString() })
    .eq("id", bidId);

  // 이력 기록
  await supabase.from("bid_notice_status_history").insert({
    bid_notice_id: bidId,
    previous_status: prevStatus,
    new_status: newStatus,
    detected_at: new Date().toISOString(),
  });

  // 알림 DB 저장 (앱에서 조회용)
  await supabase.from("notifications").insert({
    user_id: userId,
    watched_bid_id: bidId,
    message: `공고 상태가 ${prevStatus} → ${newStatus}으로 변경되었습니다.`,
  });

  // FCM 전송
  const { data: userData } = await supabase
    .from("users")
    .select("fcm_token")
    .eq("id", userId)
    .single();

  if (userData?.fcm_token && fcmToken) {
    const projectId = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON).project_id;
    await axios.post(
      `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
      {
        message: {
          token: userData.fcm_token,
          notification: {
            title: `공고 상태 변경`,
            body: `공고 ${bidNtceNo} 상태: ${prevStatus} → ${newStatus}`,
          },
          data: {
            bid_ntce_no: bidNtceNo,
            new_status: newStatus,
            watched_bid_id: bidId,
          },
        },
      },
      { headers: { Authorization: `Bearer ${fcmToken.token}` } }
    );
  }
}

main().catch(console.error);