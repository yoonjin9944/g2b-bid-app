package com.g2b.bidapp.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FmtG2bFull = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
private val FmtG2bShort = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
private val FmtG2bIso = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val FmtG2bIsoT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
private val FmtDisplay = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
private val FmtDate = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val FmtApiDate = DateTimeFormatter.ofPattern("yyyyMMdd")
private val FmtNotification = DateTimeFormatter.ofPattern("MM.dd HH:mm")
private val ZONE_SEOUL = ZoneId.of("Asia/Seoul")

// ── G2B API 문자열 ─────────────────────────────────────────────────────────────

/** G2B API 날짜 문자열(yyyyMMddHHmm[ss] / yyyy-MM-dd HH:mm:ss / ISO with T) → LocalDateTime */
fun String.parseG2bDateTime(): LocalDateTime? = try {
    when {
        contains('T') -> LocalDateTime.parse(take(19), FmtG2bIsoT)
        contains('-') -> LocalDateTime.parse(take(19), FmtG2bIso)
        length >= 14  -> LocalDateTime.parse(take(14), FmtG2bFull)
        else          -> LocalDateTime.parse(take(12), FmtG2bShort)
    }
} catch (_: Exception) {
    null
}

/** G2B API 날짜 문자열 → "yyyy.MM.dd HH:mm". 파싱 실패 시 원본 반환 */
fun String.toDisplayDateTime(): String =
    parseG2bDateTime()?.format(FmtDisplay) ?: this

fun String.toDisplayDate(): String =
    parseG2bDateTime()?.format(FmtDate) ?: this

/** G2B API 날짜 문자열 → Unix ms (KST +09:00). 파싱 실패 시 null */
fun String?.toG2bMillis(): Long? =
    this?.parseG2bDateTime()?.atZone(ZONE_SEOUL)?.toInstant()?.toEpochMilli()

/** G2B API 날짜 문자열 → ISO 8601 오프셋 문자열 (Supabase timestamp 저장용). 파싱 실패 시 null */
fun String?.toG2bIso8601(): String? =
    this?.parseG2bDateTime()?.atZone(ZONE_SEOUL)?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

/** ISO 8601 오프셋 문자열 → Unix ms. Supabase/PostgREST 반환값 처리용. 파싱 실패 시 null */
fun String?.parseIsoToMillis(): Long? = this?.let { raw ->
    try {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    } catch (_: Exception) {
        try { Instant.parse(raw).toEpochMilli() } catch (_: Exception) { null }
    }
}

// ── Unix ms ───────────────────────────────────────────────────────────────────

/** Unix ms → LocalDateTime (Asia/Seoul) */
fun Long.toSeoulDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZONE_SEOUL).toLocalDateTime()

/** Unix ms → LocalDateTime? (null-safe) */
fun Long?.toSeoulDateTime(): LocalDateTime? = this?.toSeoulDateTime()

/** Unix ms → "yyyy.MM.dd HH:mm". null이면 "-" */
fun Long?.toDisplayDateTime(): String =
    this?.toSeoulDateTime()?.format(FmtDisplay) ?: "-"

/** Unix ms → "yyyy.MM.dd" (DatePicker 날짜 표시용, 시간 제외) */
fun Long.toDisplayDate(): String =
    Instant.ofEpochMilli(this).atZone(ZONE_SEOUL).toLocalDate().format(FmtDate)

/** Unix ms → "MM.dd HH:mm" (알림 목록 시간 표시용) */
fun Long.toNotificationTime(): String =
    toSeoulDateTime().format(FmtNotification)

/** Unix ms → "yyyyMMdd0000" 또는 "yyyyMMdd2359" (G2B API 조회 파라미터용) */
fun Long.toApiQueryDate(endOfDay: Boolean): String {
    val date = Instant.ofEpochMilli(this).atZone(ZONE_SEOUL).toLocalDate()
    return "${date.format(FmtApiDate)}${if (endOfDay) "2359" else "0000"}"
}

// ── 금액 ──────────────────────────────────────────────────────────────────────

/** Long → "N억 N만원" 형식. 0 이하면 "-" */
fun Long.toPriceLabel(): String {
    if (this <= 0L) return "-"
    val uk = this / 100_000_000L
    val man = (this % 100_000_000L) / 10_000L
    return buildString {
        if (uk > 0) append("${uk}억 ")
        if (man > 0) append("${man}만")
        if (uk == 0L && man == 0L) append("${this@toPriceLabel}원")
        else append("원")
    }.trim()
}
