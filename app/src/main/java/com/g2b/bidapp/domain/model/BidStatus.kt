package com.g2b.bidapp.domain.model

enum class BidStatus {
    REGISTERED,
    CHANGED,
    CANCELLED,
    REOPENED,
    OPENED,
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