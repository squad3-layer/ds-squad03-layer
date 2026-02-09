package com.domleondev.designsystem.domain.renderer

import android.view.ViewGroup
import com.domleondev.designsystem.domain.model.ScreenDefinition

interface UiRenderer {
    fun render(container: ViewGroup, screen: ScreenDefinition)
}