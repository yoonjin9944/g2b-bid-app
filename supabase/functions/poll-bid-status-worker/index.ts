// 워커: 오케스트레이터로부터 받은 배치(최대 50건)를 실제로 처리

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { create, getNumericDate } from "https://deno.land/x/djwt@v3.0.1/mod.ts";

const CLOSING_SOON_MIN_MS = 23 * 60 * 60 * 1000;
const CLOSING_SOON_MAX_MS = 24 * 60 * 60 * 1000;
const JUST_CLOSED_MAX_MS  =      60 * 60 * 1000;

Deno.serve(async (req) => {
    try {
        const { bids, allBids, batchIndex } = await req.json();
        await processBatch(bids, allBids, batchIndex);
        return new Response(JSON.stringify({ ok: true, batchIndex }), { status: 200 });
    } catch (e) {
        console.error(e);
        return new Response(JSON.stringify({ error: String(e) }), { status: 500 });
    }
});

async function processBatch(
    bids: Record<string, unknown>[],
    allBids: Record<string, unknown>[],
    batchIndex: number,
) {
    const supabase = createClient(
        Deno.env.get("SUPABASE_URL")!,
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );

    // [진단] FCM 토큰 발급 확인
    const fcmToken = await getFcmAccessToken();
    console.log(`[진단] FCM 액세스 토큰: ${fcmToken?.token ? "발급 성공" : "발급 실패 (token 없음)"}`);
    if (!fcmToken?.token) {
        console.error("[진단] FCM 토큰 응답 내용:", JSON.stringify(fcmToken));
    }

    const now = Date.now();
    console.log(`워커 배치 #${batchIndex}: ${bids.length}건 처리 시작`);

    for (const bid of bids) {
        await new Promise((r) => setTimeout(r, 50));

        const bidNtceNo = bid.bid_ntce_no as string;
        const subscribers = allBids.filter((b) => b.bid_ntce_no === bidNtceNo);
        const clseDtMs = bid.bid_clse_dt ? new Date(bid.bid_clse_dt as string).getTime() : null;
        const opengDtMs = bid.openg_dt   ? new Date(bid.openg_dt as string).getTime()   : null;

        // [진단] 각 공고 처리 현황
        console.log(`[진단] 공고 ${bidNtceNo} | 현재상태: ${bid.current_status} | 구독자: ${subscribers.length}명 | clseDt: ${bid.bid_clse_dt} | opengDt: ${bid.openg_dt}`);

        // A. 조달청 API 기반 공고 변경 감지
        const apiStatus = await fetchBidStatus(bidNtceNo, bid.bid_category as string);
        console.log(`[진단] 공고 ${bidNtceNo} | API 상태: ${apiStatus} | DB 상태: ${bid.current_status} | 변경여부: ${apiStatus && bid.current_status !== apiStatus}`);

        for (const sub of subscribers) {
            if (apiStatus && sub.current_status !== apiStatus) {
                console.log(`[진단] 상태 변경 감지 → updateStatus 호출: ${sub.current_status} → ${apiStatus}`);
                await updateStatus(
                    supabase,
                    sub.id as string, sub.user_id as string,
                    apiStatus, sub.current_status as string,
                    bidNtceNo, bid.bid_ntce_nm as string,
                    fcmToken,
                );
            } else {
                await supabase.from("bid_notices")
                    .update({ last_checked_at: new Date().toISOString() })
                    .eq("id", sub.id);
            }
        }

        // B. 날짜 기반 알림
        for (const sub of subscribers) {
            if (clseDtMs !== null) {
                const remaining = clseDtMs - now;
                const remainingHours = Math.round(remaining / 3600000 * 10) / 10;
                console.log(`[진단] 공고 ${bidNtceNo} | 마감까지 ${remainingHours}시간 남음`);

                if (remaining > CLOSING_SOON_MIN_MS && remaining <= CLOSING_SOON_MAX_MS) {
                    console.log(`[진단] 마감 임박 알림 발송`);
                    await sendDateAlert(
                        supabase, sub.id as string, sub.user_id as string,
                        bidNtceNo, bid.bid_ntce_nm as string,
                        "마감 임박", "투찰 마감까지 24시간 미만 남았습니다", fcmToken,
                    );
                }

                if (now > clseDtMs && (now - clseDtMs) <= JUST_CLOSED_MAX_MS) {
                    console.log(`[진단] 투찰 마감 알림 발송`);
                    await sendDateAlert(
                        supabase, sub.id as string, sub.user_id as string,
                        bidNtceNo, bid.bid_ntce_nm as string,
                        "투찰 마감", "투찰이 마감되었습니다", fcmToken,
                    );
                }
            }

            if (opengDtMs !== null && now > opengDtMs && sub.current_status !== "OPENED") {
                console.log(`[진단] 개찰 감지 → OPENED 상태 업데이트`);
                await updateStatus(
                    supabase, sub.id as string, sub.user_id as string,
                    "OPENED", sub.current_status as string,
                    bidNtceNo, bid.bid_ntce_nm as string, fcmToken,
                );
            }
        }
    }

    console.log(`워커 배치 #${batchIndex}: 완료`);
}

async function getFcmAccessToken(): Promise<{ token: string }> {
    const sa = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")!);
    const privateKey = await crypto.subtle.importKey(
        "pkcs8",
        pemToBinary(sa.private_key),
        { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
        false,
        ["sign"],
    );
    const now = getNumericDate(0);
    const jwt = await create(
        { alg: "RS256", typ: "JWT" },
        {
            iss: sa.client_email,
            sub: sa.client_email,
            aud: "https://oauth2.googleapis.com/token",
            iat: now,
            exp: getNumericDate(60 * 60),
            scope: "https://www.googleapis.com/auth/firebase.messaging",
        },
        privateKey,
    );
    const res = await fetch("https://oauth2.googleapis.com/token", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
    });
    const json = await res.json();
    // Google 응답 필드는 access_token — 코드 전체에서 사용하는 token 필드명으로 변환
    return { token: json.access_token };
}

function pemToBinary(pem: string): ArrayBuffer {
    const b64 = pem.replace(/-----[^-]+-----/g, "").replace(/\s/g, "");
    const binary = atob(b64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    return bytes.buffer;
}

async function fetchBidStatus(bidNtceNo: string, category: string): Promise<string | null> {
    const endpoints: Record<string, string> = {
        CNSTWK: "getBidPblancListInfoCnstwk",
        SERVC:  "getBidPblancListInfoServc",
        THNG:   "getBidPblancListInfoThng",
    };
    const endpoint = endpoints[category] ?? "getBidPblancListInfoCnstwk";
    try {
        const url = new URL(`https://apis.data.go.kr/1230000/ad/BidPublicInfoService/${endpoint}`);
        url.searchParams.set("serviceKey", Deno.env.get("GOGOV_API_KEY")!);
        url.searchParams.set("type", "json");
        url.searchParams.set("inqryDiv", "2");
        url.searchParams.set("bidNtceNo", bidNtceNo);
        const res = await fetch(url.toString());
        const json = await res.json();
        const ntceKindNm = json?.response?.body?.items?.[0]?.ntceKindNm;
        return mapNtceKindNm(ntceKindNm);
    } catch {
        return null;
    }
}

function mapNtceKindNm(ntceKindNm: string | undefined): string | null {
    if (!ntceKindNm) return null;
    if (ntceKindNm.includes("등록"))   return "REGISTERED";
    if (ntceKindNm.includes("변경"))   return "CHANGED";
    if (ntceKindNm.includes("취소"))   return "CANCELLED";
    if (ntceKindNm.includes("재공고")) return "REOPENED";
    return null;
}

async function updateStatus(
    supabase: ReturnType<typeof createClient>,
    bidId: string, userId: string, newStatus: string, prevStatus: string,
    bidNtceNo: string, bidNtceNm: string, fcmToken: { token: string },
) {
    await supabase.from("bid_notices")
        .update({ current_status: newStatus, last_checked_at: new Date().toISOString() })
        .eq("id", bidId);

    await supabase.from("bid_notice_status_history").insert({
        bid_notice_id: bidId,
        previous_status: prevStatus,
        new_status: newStatus,
        detected_at: new Date().toISOString(),
    });

    const statusLabel: Record<string, string> = {
        CHANGED:    "변경공고",
        CANCELLED:  "취소공고",
        REOPENED:   "재공고",
        OPENED:     "개찰",
        BID_CLOSED: "투찰 마감",
        FAILED_BID: "유찰",
        AWARDED:    "낙찰",
    };
    const body = `${statusLabel[newStatus] ?? newStatus} 처리되었습니다`;

    await supabase.from("notifications").insert({ user_id: userId, watched_bid_id: bidId, message: body });
    await sendPush(supabase, userId, bidId, bidNtceNo, bidNtceNm, "공고 상태 변경", body, newStatus, fcmToken);
}

async function sendDateAlert(
    supabase: ReturnType<typeof createClient>,
    bidId: string, userId: string, bidNtceNo: string, bidNtceNm: string,
    title: string, body: string, fcmToken: { token: string },
) {
    await supabase.from("notifications").insert({ user_id: userId, watched_bid_id: bidId, message: body });
    await sendPush(supabase, userId, bidId, bidNtceNo, bidNtceNm, title, body, null, fcmToken);
}

async function sendPush(
    supabase: ReturnType<typeof createClient>,
    userId: string, bidId: string, bidNtceNo: string, bidNtceNm: string,
    title: string, body: string, newStatus: string | null, fcmToken: { token: string },
) {
    const { data: tokens } = await supabase
        .from("user_fcm_tokens").select("fcm_token").eq("user_id", userId);

    console.log(`[진단] sendPush | userId: ${userId} | FCM 기기 토큰 수: ${tokens?.length ?? 0} | FCM 액세스 토큰 존재: ${!!fcmToken?.token}`);
    if (!tokens || tokens.length === 0 || !fcmToken?.token) return;

    const sa = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")!);
    const projectId = sa.project_id;

    for (const { fcm_token } of tokens) {
        try {
            const res = await fetch(
                `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
                {
                    method: "POST",
                    headers: {
                        "Authorization": `Bearer ${fcmToken.token}`,
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({
                        message: {
                            token: fcm_token,
                            android: { priority: "high" },
                            data: {
                                title: bidNtceNm ?? title,
                                body,
                                bid_ntce_no: bidNtceNo,
                                watched_bid_id: bidId,
                                ...(newStatus && { new_status: newStatus }),
                            },
                        },
                    }),
                },
            );
            if (!res.ok) {
                const err = await res.json();
                console.error("FCM 전송 실패:", err);
                const status = err?.error?.status;
                if (status === "INVALID_ARGUMENT" || status === "UNREGISTERED") {
                    await supabase.from("user_fcm_tokens").delete().eq("fcm_token", fcm_token);
                }
            }
        } catch (e) {
            console.error("FCM fetch 오류:", e);
        }
    }
}
