package com.g2b.bidapp.domain.model

enum class BidStatus {
    REGISTERED,     // 신규 공고
    CHANGED,        // 정정공고
    CANCELLED,      // 취소공고
    BID_CLOSED,     // 입찰마감 (bidClseDt 경과)
    OPENED,         // 개찰
    FAILED_BID,     // 유찰 (scsbidNm == null)
    AWARDED,        // 낙찰 (scsbidNm 존재)
    REOPENED,       // 재공고
    ;

    companion object {
        fun fromNtceKindNm(ntceKindNm: String?): BidStatus = when (ntceKindNm) {
            "변경" -> CHANGED
            "취소" -> CANCELLED
            "재공고" -> REOPENED
            else -> REGISTERED
        }
    }
}