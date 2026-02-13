package com.domleondev.designsystem

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
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

        val type = component.type ?: ""
        val props = component.props ?: emptyMap()

        val view = when (type) {
            "Text" -> createTextView(context, props)
            "Input" -> createInputView(context, props)
            "Button" -> createButtonView(context, props)
            else -> createErrorView(context, type)
        }
        view?.let {

            applyVisualProps(it, props, context)
            applyMargins(it, props, context)
            it.isEnabled = (props["enabled"] as? Boolean) ?: true
        }

        return view
    }

    private fun createErrorView(context: Context, type: String): View {
        return TextView(context).apply {
            text = "ERRO: Componente '$type' não mapeado"
            setTextColor(Color.RED)
            setBackgroundColor(Color.YELLOW)
            setPadding(20, 20, 20, 20)
        }
    }

    private fun createTextView(context: Context, props: Map<String, Any>?): TextView {
        return TextView(context).apply {
            text = props?.get("title")?.toString() ?: ""


            val size = (props?.get("size") as? Double)?.toFloat() ?: 16f
            textSize = size
            props?.get("textColor")?.toString()?.let { setTextColor(Color.parseColor(it)) }


            applyAlignment(this, props)

            val weight = props?.get("weight")?.toString()
            if (weight == "bold") {
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun createInputView(context: Context, props: Map<String, Any>?): DsInput {
        return DsInput(context).apply {
            hint = props?.get("hint")?.toString() ?: ""

            val height = ((props?.get("height") as? Double)?.toInt() ?: 48).dpToPx(context)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
            )

            val keyboardTypeString = props?.get("keyboardType")?.toString()?.uppercase()
            val keyboardType = when (keyboardTypeString) {
                "EMAIL" -> DsInput.KeyboardType.EMAIL
                "PASSWORD" -> DsInput.KeyboardType.PASSWORD
                "NUMBER" -> DsInput.KeyboardType.NUMBER
                "PHONE" -> DsInput.KeyboardType.PHONE
                else -> DsInput.KeyboardType.TEXT
            }
            setKeyboardType(keyboardType)

            val inputId = props?.get("id")?.toString()
            if (inputId != null) {
                id = context.resources.getIdentifier(inputId, "id", context.packageName)
            }
        }
    }

    private fun createButtonView(context: Context, props: Map<String, Any>?): DsButton {
        return DsButton(context).apply {
            text = props?.get("text")?.toString() ?: ""
            val color = props?.get("textColor")?.toString()
            color?.let { setTextColor(android.graphics.Color.parseColor(it)) }
        }
    }


    private fun applyVisualProps(view: View, props: Map<String, Any>?, context: Context) {
        val bgColor = props?.get("backgroundColor")?.toString()
        val borderRadius = (props?.get("border_radius") as? Double)?.toFloat() ?: 0f
        val borderColor = props?.get("border_color")?.toString()


        if (bgColor != null || borderColor != null || borderRadius > 0f) {
            val colorInt = if (bgColor != null) Color.parseColor(bgColor) else Color.TRANSPARENT

            if (view is DsButton) {
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
            } else {

                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(colorInt)

                    borderColor?.let {
                        setStroke(2.dpToPx(context), Color.parseColor(it))
                    }
                    cornerRadius = borderRadius.dpToPx(context).toFloat()
                }
                view.background = shape
            }
        }

        view.visibility = if (props?.get("visibility")?.toString() == "hidden") View.GONE else View.VISIBLE

        val textColor = props?.get("textColor")?.toString()
        if (textColor != null && view is TextView) {
            view.setTextColor(Color.parseColor(textColor))
        }
    }

    private fun applyAlignment(textView: TextView, props: Map<String, Any>?) {
        val align = props?.get("alignment")?.toString()?.lowercase()
        when (align) {
            "center" -> {
                textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                textView.gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            "right" -> {
                textView.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
                textView.gravity = android.view.Gravity.END
            }
            else -> {
                textView.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                textView.gravity = android.view.Gravity.START
            }
        }
    }

    private fun applyMargins(view: View, props: Map<String, Any>?, context: Context) {
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
            ?: ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        lp.setMargins(
            ((props?.get("margin_left") as? Double)?.toInt() ?: 0).dpToPx(context),
            ((props?.get("margin_top") as? Double)?.toInt() ?: 0).dpToPx(context),
            ((props?.get("margin_right") as? Double)?.toInt() ?: 0).dpToPx(context),
            ((props?.get("margin_bottom") as? Double)?.toInt() ?: 0).dpToPx(context)
        )
        view.layoutParams = lp
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun Float.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}