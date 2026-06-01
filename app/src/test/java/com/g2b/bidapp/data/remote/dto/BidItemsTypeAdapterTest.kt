package com.g2b.bidapp.data.remote.dto

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BidItemsTypeAdapterTest {

    private lateinit var gson: Gson

    @Before
    fun setUp() {
        gson = GsonBuilder()
            .registerTypeAdapter(BidNoticeItems::class.java, BidItemsTypeAdapter())
            .create()
    }

    @Test
    fun `items 가 단건 객체일 때 List 1개 반환`() {
        val json = """
            {
              "response": {
                "body": {
                  "items": { "item": { "bidNtceNo": "123", "bidNtceNm": "테스트공고" } },
                  "totalCount": 1, "pageNo": 1, "numOfRows": 20
                }
              }
            }
            """.trimIndent()

        val result = gson.fromJson(json, BidNoticeListResponse::class.java)
        val items = result.response?.body?.items?.item ?: emptyList()

        assertEquals(1, items.size)
        assertEquals("123", items[0].bidNtceNo)
    }

    @Test
    fun `items 가 배열일 때 List N개 반환`() {
        val json = """
            {
              "response": {
                "body": {
                  "items": {
                    "item": [
                      { "bidNtceNo": "001", "bidNtceNm": "공고1" },
                      { "bidNtceNo": "002", "bidNtceNm": "공고2" }
                    ]
                  },
                  "totalCount": 2, "pageNo": 1, "numOfRows": 20
                }
              }
            }
        """.trimIndent()

        val result = gson.fromJson(json, BidNoticeListResponse::class.java)
        val items = result.response?.body?.items?.item ?: emptyList()

        assertEquals(2, items.size)
        assertEquals("001", items[0].bidNtceNo)
        assertEquals("002", items[1].bidNtceNo)
    }

    @Test
    fun `items 가 빈 문자열일 때 emptyList 반환`() {
        val json = """
            {
              "response": {
                "body": {
                  "items": "",
                  "totalCount": 0, "pageNo": 1, "numOfRows": 20
                }
              }
            }
        """.trimIndent()

        val result = gson.fromJson(json, BidNoticeListResponse::class.java)
        val items = result.response?.body?.items?.item ?: emptyList()

        assertTrue(items.isEmpty())
    }

    @Test
    fun `totalCount 가 올바르게 파싱된다`() {
        val json = """
            {
              "response": {
                "body": {
                  "items": "",
                  "totalCount": 1245, "pageNo": 1, "numOfRows": 20
                }
              }
            }
        """.trimIndent()

        val result = gson.fromJson(json, BidNoticeListResponse::class.java)

        assertEquals(1245, result.response?.body?.totalCount)
    }
}