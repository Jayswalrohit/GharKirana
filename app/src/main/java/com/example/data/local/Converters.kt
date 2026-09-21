package com.example.data.local

import com.example.data.model.OrderItem
import org.json.JSONArray
import org.json.JSONObject

object OrderItemsConverter {
    fun fromList(items: List<OrderItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("productId", item.productId)
                put("productName", item.productName)
                put("unit", item.unit)
                put("price", item.price)
                put("quantity", item.quantity)
                put("total", item.total)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun toList(json: String): List<OrderItem> {
        val list = mutableListOf<OrderItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    OrderItem(
                        productId = obj.optString("productId", ""),
                        productName = obj.optString("productName", ""),
                        unit = obj.optString("unit", ""),
                        price = obj.optDouble("price", 0.0),
                        quantity = obj.optInt("quantity", 1),
                        total = obj.optDouble("total", 0.0)
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }
}
