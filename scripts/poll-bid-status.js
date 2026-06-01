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

// 날짜 기반 알림 판정 window (매시간 실행 기준으로 각 알림이 1회만 발송되도록 설정)
const CLOSING_SOON_MIN_MS = 23 * 60 * 60 * 1000; // 23시간
const CLOSING_SOON_MAX_MS = 24 * 60 * 60 * 1000; // 24시간
const JUST_CLOSED_MAX_MS  =      60 * 60 * 1000; //  1시간

async function getFcmAccessToken() {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
  const auth = new GoogleAuth({
    credentials: serviceAccount,
    scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
  });
  return (await auth.getClient()).getAccessToken();
}

async function main() {
  // 1. 폴링 대상 관심공고 조회 (날짜 필드 포함)
  const { data: bids } = await supabase
    .from("bid_notices")
    .select("id, user_id, bid_ntce_no, bid_ntce_nm, bid_category, current_status, bid_clse_dt, openg_dt")
    .not("current_status", "in", '("OPENED","CANCELLED")');

  if (!bids || bids.length === 0) {
    console.log("No bids to poll.");
    await supabase.from("bid_notices").select("id").limit(1); // Ping
    return;
  }

  // 2. bid_ntce_no 중복 제거 (API 호출 최소화)
  const unique = [...new Map(bids.map((b) => [b.bid_ntce_no, b])).values()];

  const fcmToken = await getFcmAccessToken();
  const now = Date.now();

  for (const bid of unique) {
    await new Promise((r) => setTimeout(r, 50)); // 30 tps 제한 대응

    const subscribers = bids.filter((b) => b.bid_ntce_no === bid.bid_ntce_no);
    const clseDtMs = bid.bid_clse_dt ? new Date(bid.bid_clse_dt).getTime() : null;
    const opengDtMs = bid.openg_dt   ? new Date(bid.openg_dt).getTime()   : null;

    // A. 조달청 API 기반 공고 변경 감지 (변경공고/취소공고/재공고)
    const apiStatus = await fetchBidStatus(bid.bid_ntce_no, bid.bid_category);
    for (const sub of subscribers) {
      if (apiStatus && sub.current_status !== apiStatus) {
        await updateStatus(sub.id, sub.user_id, apiStatus, sub.current_status, bid.bid_ntce_no, bid.bid_ntce_nm, fcmToken);
      } else {
        await supabase.from("bid_notices").update({ last_checked_at: new Date().toISOString() }).eq("id", sub.id);
      }
    }

    // B. 날짜 기반 알림
    for (const sub of subscribers) {
      if (clseDtMs !== null) {
        const remaining = clseDtMs - now;

        // B-1. 마감 임박: 23~24시간 이내 (1시간 window → 1회만 발송)
        if (remaining > CLOSING_SOON_MIN_MS && remaining <= CLOSING_SOON_MAX_MS) {
          await sendDateAlert(
            sub.id, sub.user_id, bid.bid_ntce_no, bid.bid_ntce_nm,
            "마감 임박", "투찰 마감까지 24시간 미만 남았습니다",
            fcmToken
          );
        }

        // B-2. 투찰 마감: 마감 후 1시간 이내 (1시간 window → 1회만 발송)
        if (now > clseDtMs && (now - clseDtMs) <= JUST_CLOSED_MAX_MS) {
          await sendDateAlert(
            sub.id, sub.user_id, bid.bid_ntce_no, bid.bid_ntce_nm,
            "투찰 마감", "투찰이 마감되었습니다",
            fcmToken
          );
        }
      }

      // B-3. 개찰: opengDt 경과 + 아직 OPENED 아님 → current_status 업데이트 + 알림
      if (opengDtMs !== null && now > opengDtMs && sub.current_status !== "OPENED") {
        await updateStatus(
          sub.id, sub.user_id, "OPENED", sub.current_status,
          bid.bid_ntce_no, bid.bid_ntce_nm, fcmToken
        );
      }
    }
  }

  // Supabase Ping (Free 플랜 자동 일시정지 방지)
  await supabase.from("bid_notices").select("id").limit(1);
  console.log("Polling complete.");
}

async function fetchBidStatus(bidNtceNo, category) {
  const endpoints = {
    CNSTWK: "getBidPblancListInfoCnstwk",
    SERVC:  "getBidPblancListInfoServc",
    THNG:   "getBidPblancListInfoThng",
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

// 공고 상태 변경 (DB 업데이트 + 이력 + 알림)
async function updateStatus(bidId, userId, newStatus, prevStatus, bidNtceNo, bidNtceNm, fcmToken) {
  await supabase
    .from("bid_notices")
    .update({ current_status: newStatus, last_checked_at: new Date().toISOString() })
    .eq("id", bidId);

  await supabase.from("bid_notice_status_history").insert({
    bid_notice_id: bidId,
    previous_status: prevStatus,
    new_status: newStatus,
    detected_at: new Date().toISOString(),
  });

  const statusLabel = { CHANGED: "변경공고", CANCELLED: "취소공고", REOPENED: "재공고", OPENED: "개찰" }[newStatus] ?? newStatus;
  const body = `${statusLabel} 처리되었습니다`;

  await supabase.from("notifications").insert({
    user_id: userId,
    watched_bid_id: bidId,
    message: body,
  });

  await sendPush(userId, bidId, bidNtceNo, bidNtceNm, "공고 상태 변경", body, newStatus, fcmToken);
}

// 날짜 기반 알림 (DB 상태 변경 없음, 알림만 발송)
async function sendDateAlert(bidId, userId, bidNtceNo, bidNtceNm, title, body, fcmToken) {
  await supabase.from("notifications").insert({
    user_id: userId,
    watched_bid_id: bidId,
    message: body,
  });

  await sendPush(userId, bidId, bidNtceNo, bidNtceNm, title, body, null, fcmToken);
}

// FCM 전송
async function sendPush(userId, bidId, bidNtceNo, bidNtceNm, title, body, newStatus, fcmToken) {
  const { data: userData } = await supabase
    .from("users")
    .select("fcm_token")
    .eq("id", userId)
    .single();

  if (!userData?.fcm_token || !fcmToken) return;

  const projectId = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON).project_id;
  await axios.post(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    {
      message: {
        token: userData.fcm_token,
        android: { priority: "high" }, // 종료 상태에서도 도달 보장
        data: {
          title: bidNtceNm ?? title,
          body,
          bid_ntce_no: bidNtceNo,
          watched_bid_id: bidId,
          ...(newStatus && { new_status: newStatus }),
        },
      },
    },
    { headers: { Authorization: `Bearer ${fcmToken.token}` } }
  );
}

main().catch(console.error);
