package com.g2b.bidapp.data.remote.dto

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

// BidItemsTypeAdapter와 동일 패턴 — BidResultItems 전용
class BidResultItemsTypeAdapter : JsonDeserializer<BidResultItems> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): BidResultItems {
        if (json.isJsonNull || json.isJsonPrimitive) return BidResultItems(emptyList())

        if (json.isJsonArray) {
            val list: List<BidResultDto> = context.deserialize(
                json,
                object : TypeToken<List<BidResultDto>>() {}.type,
            )
            return BidResultItems(list)
        }

        val itemEl = json.asJsonObject.get("item") ?: return BidResultItems(emptyList())

        val list: List<BidResultDto> = when {
            itemEl.isJsonNull -> emptyList()
            itemEl.isJsonObject -> listOf(
                context.deserialize(itemEl, BidResultDto::class.java),
            )
            itemEl.isJsonArray -> context.deserialize(
                itemEl,
                object : TypeToken<List<BidResultDto>>() {}.type,
            )
            else -> emptyList()
        }

        return BidResultItems(list)
    }
}