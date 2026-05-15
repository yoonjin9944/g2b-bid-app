package com.g2b.bidapp.data.remote.dto

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class BidItemsTypeAdapter : JsonDeserializer<BidNoticeItems> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): BidNoticeItems {
        if (json.isJsonNull || json.isJsonPrimitive) return BidNoticeItems(emptyList())

        val itemEl = json.asJsonObject.get("item") ?: return BidNoticeItems(emptyList())

        val list: List<BidNoticeDto> = when {
            itemEl.isJsonNull -> emptyList()
            itemEl.isJsonObject -> listOf(
                context.deserialize(itemEl, BidNoticeDto::class.java)
            )
            itemEl.isJsonArray -> context.deserialize(
                itemEl,
                object : TypeToken<List<BidNoticeDto>>() {}.type
            )

            else -> emptyList()
        }

        return BidNoticeItems(list)
    }

}