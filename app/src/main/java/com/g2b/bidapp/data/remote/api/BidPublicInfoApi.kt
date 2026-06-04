package com.g2b.bidapp.data.remote.api

import com.g2b.bidapp.data.remote.dto.BidNoticeListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BidPublicInfoApi {

    @GET("ad/BidPublicInfoService/getBidPblancListInfoCnstwkPPSSrch")
    suspend fun getCnstwkList(
        @Query("inqryDiv") inqryDiv: Int = 1, // 등록일시 기준 조회
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("bidNtceNm") bidNtceNm: String? = null,
        @Query("bidNtceNo") bidNtceNo: String? = null,
        @Query("dmInsttNm") dmInsttNm: String? = null,
        @Query("inqryBgnDt") inqryBgnDt: String? = null,
        @Query("inqryEndDt") inqryEndDt: String? = null,
    ): BidNoticeListResponse

    @GET("ad/BidPublicInfoService/getBidPblancListInfoServcPPSSrch")
    suspend fun getServcList(
        @Query("inqryDiv") inqryDiv: Int = 1,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("bidNtceNm") bidNtceNm: String? = null,
        @Query("bidNtceNo") bidNtceNo: String? = null,
        @Query("dmInsttNm") dmInsttNm: String? = null,
        @Query("inqryBgnDt") inqryBgnDt: String? = null,
        @Query("inqryEndDt") inqryEndDt: String? = null,
    ): BidNoticeListResponse

    @GET("ad/BidPublicInfoService/getBidPblancListInfoThngPPSSrch")
    suspend fun getThngList(
        @Query("inqryDiv") inqryDiv: Int = 1,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("bidNtceNm") bidNtceNm: String? = null,
        @Query("bidNtceNo") bidNtceNo: String? = null,
        @Query("dmInsttNm") dmInsttNm: String? = null,
        @Query("inqryBgnDt") inqryBgnDt: String? = null,
        @Query("inqryEndDt") inqryEndDt: String? = null,
    ): BidNoticeListResponse
}