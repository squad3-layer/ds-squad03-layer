package com.domleondev.designsystem

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.marginBottom
import com.domleondev.designsystem.domain.model.Component
import com.example.mylibrary.ds.input.DsInput
import com.example.mylibrary.ds.button.DsButton
import coil.load
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComponentFactory @Inject constructor() {

    fun createView(context: Context, component: Component): View? {

        val type = component.type ?: ""
        val props = component.props ?: emptyMap()

        val view = when (type) {
            "Header" -> createHeaderView(context, props)
            "MenuItem" -> createMenuItemView(context, props)
            "HorizontalContainer" -> createHorizontalContainer(context, component)
            "Text" -> createTextView(context, props)
            "Input" -> createInputView(context, props)
            "Button" -> createButtonView(context, props)
            "NewsCard" -> createNewsCard(context, props)
            "IconButton" -> createIconButtonView(context, props)
            "VerticalContainer" -> createVerticalContainer(context, component)
            "SelectableChip" -> createChipView(context, props)
            "FlowContainer" -> createFlowContainer(context)
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

    private fun createIconButtonView(context: Context, props: Map<String, Any>?): View {
        return android.widget.ImageView(context).apply {
            val iconName = props?.get("icon")?.toString() ?: "ic_filter"
            val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (resId != 0) setImageResource(resId)

            layoutParams = LinearLayout.LayoutParams(40.dpToPx(context), 40.dpToPx(context))
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE

            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
        }
    }

    private fun createHorizontalContainer(context: Context, component: Component): LinearLayout {
        val props = component.props ?: emptyMap()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
    }

    private fun createVerticalContainer(context: Context, component: Component): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
    }

    private fun createChipView(context: Context, props: Map<String, Any>?): TextView {
        return TextView(context).apply {
            id = View.generateViewId()
            text = props?.get("text")?.toString() ?: ""
            textSize = 10f
            gravity = android.view.Gravity.CENTER
            setPadding(24.dpToPx(context), 8.dpToPx(context), 24.dpToPx(context), 8.dpToPx(context))

            val isSelected = props?.get("selected") as? Boolean ?: false

            updateChipStyle(this, isSelected)

            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                marginEnd = 12.dpToPx(context)
                bottomMargin = 14.dpToPx(context)
            }
        }
    }

    fun updateChipStyle(view: TextView, isSelected: Boolean) {
        val context = view.context
        val backgroundColor = if (isSelected) "#0056D2" else "#F3F3F3"
        val textColor = if (isSelected) "#FFFFFF" else "#000000"

        view.setTextColor(Color.parseColor(textColor))

        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 19.dpToPx(context).toFloat()
            setColor(Color.parseColor(backgroundColor))
        }
        view.background = shape
    }

    private fun createFlowContainer(context: Context): com.google.android.material.chip.ChipGroup {
        return com.google.android.material.chip.ChipGroup(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            isSingleLine = false

            chipSpacingHorizontal = 8.dpToPx(context)
            chipSpacingVertical = 8.dpToPx(context)

            isSingleSelection = true
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
                "CPF" -> DsInput.KeyboardType.CPF
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

    private fun createHeaderView(context: Context, props: Map<String, Any>?): View {
        return androidx.constraintlayout.widget.ConstraintLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0.dpToPx(context), 28.dpToPx(context), 0.dpToPx(context), 0.dpToPx(context))

            val leftIcon = android.widget.ImageView(context).apply {
                id = View.generateViewId()
                val showBack = props?.get("showBack") as? Boolean ?: false
                val showMenu = props?.get("showMenu") as? Boolean ?: false

                val iconRes = when {
                    showBack -> context.resources.getIdentifier("ic_back", "drawable", context.packageName)
                    showMenu -> context.resources.getIdentifier("ic_menu", "drawable", context.packageName)
                    else -> 0
                }

                if (iconRes != 0) {
                    setImageResource(iconRes)
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }

                layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                    24.dpToPx(context), 24.dpToPx(context)
                ).apply {
                    startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                }
            }

            val titleView = TextView(context).apply {
                id = View.generateViewId()
                text = props?.get("title")?.toString() ?: ""

                val colorHex = props?.get("textColor")?.toString() ?: "#000000"
                try {
                    setTextColor(android.graphics.Color.parseColor(colorHex))
                } catch (e: Exception) {
                    setTextColor(android.graphics.Color.BLACK)
                }

                val customSize = (props?.get("titleSize") as? Double)?.toFloat() ?: 18f
                textSize = customSize

                val fontName = props?.get("typeface")?.toString()
                if (fontName != null) {
                    val fontResId = context.resources.getIdentifier(fontName, "font", context.packageName)
                    if (fontResId != 0) {
                        typeface = androidx.core.content.res.ResourcesCompat.getFont(context, fontResId)
                    }
                } else {
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }

                visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE

                layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    val marginStart = if (leftIcon.visibility == View.VISIBLE) 12.dpToPx(context) else 0

                    startToEnd = leftIcon.id
                    topToTop = leftIcon.id
                    bottomToBottom = leftIcon.id
                    setMargins(marginStart, 0, 0, 0)
                }
            }

            addView(leftIcon)
            addView(titleView)
        }
    }

    private fun createNewsCard(context: Context, props: Map<String, Any>?): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            setPadding(12.dpToPx(context), 12.dpToPx(context), 12.dpToPx(context), 12.dpToPx(context))

            val imageView = android.widget.ImageView(context).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(100.dpToPx(context), 100.dpToPx(context))
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP

                val url = props?.get("imageUrl")?.toString()
                if (!url.isNullOrEmpty()) {
                    this.load(url) {
                        crossfade(true)
                        placeholder(android.R.drawable.progress_indeterminate_horizontal)
                    }
                }
            }

            val textContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                    marginStart = 12.dpToPx(context)
                }

                val titleView = TextView(context).apply {
                    text = props?.get("title")?.toString() ?: ""
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    maxLines = 2
                }

                val descView = TextView(context).apply {
                    text = props?.get("description")?.toString() ?: ""
                    textSize = 14f
                    maxLines = 3
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }

                addView(titleView)
                addView(descView)
            }

            addView(imageView)
            addView(textContainer)
        }
    }

    private fun createMenuItemView(context: Context, props: Map<String, Any>?): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            setPadding(24.dpToPx(context), 16.dpToPx(context), 24.dpToPx(context), 16.dpToPx(context))


            isClickable = true
            isFocusable = true
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)


            val iconView = android.widget.ImageView(context).apply {
                val iconName = props?.get("icon")?.toString()
                if (iconName != null) {
                    val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                    if (resId != 0) setImageResource(resId)
                }
                layoutParams = LinearLayout.LayoutParams(24.dpToPx(context), 24.dpToPx(context))

                val iconColor = props?.get("iconColor")?.toString() ?: "#424242"
                setColorFilter(Color.parseColor(iconColor))
            }

            val textView = TextView(context).apply {
                text = props?.get("text")?.toString() ?: ""
                textSize = 16f
                val colorHex = props?.get("textColor")?.toString() ?: "#000000"
                setTextColor(Color.parseColor(colorHex))

                val fontName = props?.get("typeface")?.toString()
                if (fontName != null) {
                    val fontResId = context.resources.getIdentifier(fontName, "font", context.packageName)
                    if (fontResId != 0) typeface = androidx.core.content.res.ResourcesCompat.getFont(context, fontResId)
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = 24.dpToPx(context)}
            }

            addView(iconView)
            addView(textView)
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

        val flex = (props?.get("flex") as? Number)?.toFloat() ?: 0f

        if (flex > 0 && lp is LinearLayout.LayoutParams) {
            lp.weight = flex
            lp.width = 0
        }

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