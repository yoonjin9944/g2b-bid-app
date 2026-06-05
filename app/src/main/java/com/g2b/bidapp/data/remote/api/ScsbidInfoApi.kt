package com.g2b.bidapp.data.remote.api

import com.g2b.bidapp.data.remote.dto.BidResultListResponse
import retrofit2.http.GET
import retrofit2.http.Query

// 낙찰결과 조회 API (조달청 ScsbidInfoService)
// Base URL: https://apis.data.go.kr/1230000/ad/
// ServiceKey + type=json 은 AuthInterceptor가 자동 주입
interface ScsbidInfoApi {

//    @GET("as/ScsbidInfoService/getScsbidListSttusCnstwk")
    @GET("as/ScsbidInfoService/getScsbidListSttusCnstwkPPSSrch")
    suspend fun getCnstwkList(
        @Query("inqryDiv") inqryDiv: Int = 1,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("inqryBgnDt") inqryBgnDt: String? = null,  // yyyyMMddHHmm
        @Query("inqryEndDt") inqryEndDt: String? = null,  // yyyyMMddHHmm
        @Query("bidNtceNm") bidNtceNm: String? = null,
        @Query("ntceInsttNm") ntceInsttNm: String? = null,
    ): BidResultListResponse

//    @GET("as/ScsbidInfoService/getScsbidListSttusServc")
    @GET("as/ScsbidInfoService/getScsbidListSttusServcPPSSrch")
    suspend fun getServcList(
        @Query("inqryDiv") inqryDiv: Int = 1,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("inqryBgnDt") inqryBgnDt: String? = null,
        @Query("inqryEndDt") inqryEndDt: String? = null,
        @Query("bidNtceNm") bidNtceNm: String? = null,
        @Query("ntceInsttNm") ntceInsttNm: String? = null,
    ): BidResultListResponse

//    @GET("as/ScsbidInfoService/getScsbidListSttusThng")
    @GET("as/ScsbidInfoService/getScsbidListSttusThngPPSSrch")
    suspend fun getThngList(
        @Query("inqryDiv") inqryDiv: Int = 1,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("inqryBgnDt") inqryBgnDt: String? = null,
        @Query("inqryEndDt") inqryEndDt: String? = null,
        @Query("bidNtceNm") bidNtceNm: String? = null,
        @Query("ntceInsttNm") ntceInsttNm: String? = null,
    ): BidResultListResponse
}