package com.domleondev.designsystem.domain.usecase

import com.domleondev.designsystem.domain.model.ScreenDefinition
import com.domleondev.designsystem.parser.JsonParser
import javax.inject.Inject

class RenderScreenUseCase @Inject constructor(
    private val jsonParser: JsonParser
) {
    operator fun invoke(json: String): ScreenDefinition? {
        return if (json.isNotEmpty()) {
            jsonParser.parseScreen(json)
        } else {
            null
        }
    }
}