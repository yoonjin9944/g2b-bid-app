package com.g2b.bidapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchParams(
    val keyword: String = "",
    val bidNtceNo: String = "",
    val dmInsttNm: String = "",
    val category: BidCategory? = null,
    val inqryBgnDt: String = "",
    val inqryEndDt: String = "",
) : Parcelable {
    val isEmpty: Boolean
        get() = keyword.isBlank() && bidNtceNo.isBlank() && dmInsttNm.isBlank()
                && category == null && inqryBgnDt.isBlank() && inqryEndDt.isBlank()
}
