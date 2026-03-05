package com.domleondev.designsystem.parser

import com.domleondev.designsystem.domain.model.ScreenDefinition
import com.google.gson.Gson
import javax.inject.Inject

class GsonJsonParser @Inject constructor(
    private val gson: Gson
) : JsonParser {

    override fun parseScreen(json: String): ScreenDefinition? {
        return try {
            gson.fromJson(json, ScreenDefinition::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}