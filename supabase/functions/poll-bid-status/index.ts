// 오케스트레이터: 전체 폴링 대상을 조회하고 BATCH_SIZE 단위로 워커에 분배

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const BATCH_SIZE = 50;

Deno.serve(async (_req) => {
    console.log("오케스트레이터 시작");
    try {
        await main();
        console.log("오케스트레이터 정상 종료");
        return new Response(JSON.stringify({ ok: true }), { status: 200 });
    } catch (e) {
        console.error("오케스트레이터 오류:", e);
        return new Response(JSON.stringify({ error: String(e) }), { status: 500 });
    }
});

async function main() {
    const supabase = createClient(
        Deno.env.get("SUPABASE_URL")!,
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );

    const { data: bids } = await supabase
        .from("bid_notices")
        .select("id, user_id, bid_ntce_no, bid_ntce_nm, bid_category, current_status, bid_clse_dt, openg_dt")
        .not("current_status", "in", '("OPENED","CANCELLED")');

    if (!bids || bids.length === 0) {
        console.log("No bids to poll.");
        return;
    }

    // bid_ntce_no 기준 중복 제거
    const unique = [...new Map(bids.map((b) => [b.bid_ntce_no, b])).values()];

    console.log(`폴링 대상: ${unique.length}건, 배치 수: ${Math.ceil(unique.length / BATCH_SIZE)}`);
    if (unique.length > 100) {
        console.warn("⚠️ 100건 초과 — 배치 분할 워커로 처리합니다.");
    }

    // BATCH_SIZE 단위로 슬라이스
    const batches: typeof unique[] = [];
    for (let i = 0; i < unique.length; i += BATCH_SIZE) {
        batches.push(unique.slice(i, i + BATCH_SIZE));
    }

    const workerUrl = `${Deno.env.get("SUPABASE_URL")}/functions/v1/poll-bid-status-worker`;
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

    // Fire-and-Forget: 워커 응답을 기다리지 않고 즉시 반환
    // → pg_net 5초 타임아웃 초과 방지
    const workerPromises = batches.map((batch, idx) =>
        fetch(workerUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${serviceRoleKey}`,
            },
            body: JSON.stringify({
                bids: batch,
                allBids: bids,
                batchIndex: idx,
            }),
        }).then(res => console.log(`배치 #${idx} 호출 완료 (HTTP ${res.status})`))
          .catch(e => console.error(`배치 #${idx} 호출 실패:`, e))
    );

    // EdgeRuntime.waitUntil: 응답 반환 후에도 워커 호출이 완료될 때까지 함수 유지
    // @ts-ignore
    if (typeof EdgeRuntime !== "undefined") {
        // @ts-ignore
        EdgeRuntime.waitUntil(Promise.allSettled(workerPromises));
    }

    console.log("오케스트레이터: 워커 호출 완료, 응답 반환.");
}
