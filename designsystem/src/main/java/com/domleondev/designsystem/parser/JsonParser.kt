package com.domleondev.designsystem.parser

import com.domleondev.designsystem.domain.model.ScreenDefinition

interface JsonParser {
    fun parseScreen(json: String): ScreenDefinition?
}