package com.domleondev.designsystem.presentation.renderer

import android.view.ViewGroup
import com.domleondev.designsystem.ComponentFactory
import com.domleondev.designsystem.domain.model.ScreenDefinition
import com.domleondev.designsystem.domain.renderer.UiRenderer
import javax.inject.Inject

class BackendDrivenUiRenderer @Inject constructor(
    private val factory: ComponentFactory
) : UiRenderer {

    override fun render(container: ViewGroup, screen: ScreenDefinition) {
        container.removeAllViews()

        screen.components.forEach { componentData ->
            val view = factory.createView(container.context, componentData)

            view?.let {
                container.addView(it)
            }
        }
    }
}