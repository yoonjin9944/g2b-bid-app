package com.g2b.bidapp.domain.model

enum class BidCategory(val label: String, val apiCode: String) {
    CNSTWK("공사", "CNSTWK"),
    SERVC("용역", "SERVC"),
    THNG("물품", "THNG"),
    ;

    companion object {
        fun fromApiCode(code: String?): BidCategory =
            entries.firstOrNull { it.apiCode == code } ?: CNSTWK
    }
}