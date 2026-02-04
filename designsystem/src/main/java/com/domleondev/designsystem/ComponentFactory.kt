package com.domleondev.designsystem

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.domleondev.designsystem.domain.model.Component
import com.example.mylibrary.ds.input.DsInput
import com.example.mylibrary.ds.button.DsButton
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComponentFactory @Inject constructor() {

    fun createView(context: Context, component: Component): View? {
        val view = when (component.type) {
            "Text" -> createTextView(context, component.props)
            "Input" -> createInputView(context, component.props)
            "Button" -> createButtonView(context, component.props)
            else -> null
        }

        view?.let { applyMargins(it, component.props, context) }

        return view
    }

    private fun createTextView(context: Context, props: Map<String, Any>?): TextView {
        return TextView(context).apply {
            text = props?.get("title")?.toString() ?: ""

            val size = (props?.get("size") as? Double)?.toFloat() ?: 16f
            textSize = size

            val weight = props?.get("weight")?.toString()
            if (weight == "bold") {
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun createInputView(context: Context, props: Map<String, Any>?): DsInput {
        return DsInput(context).apply {
            hint = props?.get("hint")?.toString() ?: ""
        }
    }

    private fun createButtonView(context: Context, props: Map<String, Any>?): DsButton {
        return DsButton(context).apply {
            text = props?.get("text")?.toString() ?: ""
        }
    }

    private fun applyMargins(view: View, props: Map<String, Any>?, context: Context) {
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
            ?: ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        val left = ((props?.get("margin_left") as? Double)?.toInt() ?: 0).dpToPx(context)
        val top = ((props?.get("margin_top") as? Double)?.toInt() ?: 0).dpToPx(context)
        val right = ((props?.get("margin_right") as? Double)?.toInt() ?: 0).dpToPx(context)
        val bottom = ((props?.get("margin_bottom") as? Double)?.toInt() ?: 0).dpToPx(context)

        lp.setMargins(left, top, right, bottom)
        view.layoutParams = lp
    }
    private fun Int.dpToPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }
}