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
import com.example.mylibrary.ds.text.DsText
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComponentFactory @Inject constructor() {

    fun createView(context: Context, component: Component): View? {

        val type = component.type ?: ""
        val props = component.props ?: emptyMap()

        val view = when (type) {
            "ProgressBar" -> createProgressBar(context, props)
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
            "FlowContainer" -> createFlowContainer(context, props)
            "DetailsImage" -> createDetailsImageView(context, props)
            "NotificationCard" -> createNotificationCard(context, props)
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

    private fun createTextView(context: Context, props: Map<String, Any>?): View {
        return com.example.mylibrary.ds.text.DsText(context).apply {
            val style = props?.get("textStyle")?.toString() ?: "text"

            setTextStyle(when(style) {
                "header" -> DsText.TextStyle.HEADER
                "subtitle" -> DsText.TextStyle.SUBTITLE
                else -> DsText.TextStyle.TEXT
            })
            text = props?.get("title")?.toString() ?: ""
        }
    }

    private fun createIconButtonView(context: Context, props: Map<String, Any>?): View {
        return com.example.mylibrary.ds.icon.DsIcon(context).apply {

            val iconName = props?.get("icon")?.toString() ?: "ds_icon_notification"
            val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (resId != 0) {
                setIcon(androidx.core.content.ContextCompat.getDrawable(context, resId))
            }

            props?.get("iconDescription")?.toString()?.let { setIconDescription(it) }
            props?.get("badgeDescription")?.toString()?.let { setBadgeDescription(it) }

            val count = (props?.get("badgeCount") as? Number)?.toInt() ?: 0
            setBadgeCount(count)

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
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

    private fun createChipView(context: Context, props: Map<String, Any>?): View {
        return com.example.mylibrary.ds.chip.DsChip(context).apply {
            setDsText(props?.get("text")?.toString() ?: "")

            props?.get("backgroundColor")?.toString()?.let { setDsBackgroundColor(Color.parseColor(it)) }
            props?.get("textColor")?.toString()?.let { setDsTextColor(Color.parseColor(it)) }

            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
    }

    private fun createFlowContainer(context: Context, props: Map<String, Any>?): View {
        return com.example.mylibrary.ds.chip.DsChipGroup(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            val spacing = (props?.get("chipSpacing") as? Number)?.toInt() ?: 12
            setChipSpacing(spacing)

            val categories = props?.get("items") as? List<String>
            categories?.let { addChips(it) }

            val selBg = props?.get("selectedBg")?.toString()
            val selText = props?.get("selectedText")?.toString()
            if (selBg != null && selText != null) {
                setChipColors(
                    selectedBg = Color.parseColor(selBg),
                    selectedText = Color.parseColor(selText),
                    unselectedBg = Color.parseColor("#F3F3F3"),
                    unselectedText = Color.parseColor("#000000")
                )
            }
        }
    }

    private fun createDetailsImageView(context: Context, props: Map<String, Any>?): View {
        return android.widget.ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 310.dpToPx(context))
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            val url = props?.get("imageUrl")?.toString()
            load(url)
        }
    }

    private fun createInputView(context: Context, props: Map<String, Any>?): DsInput {
        return DsInput(context).apply {
            hint = props?.get("hint")?.toString() ?: ""

            val height = ((props?.get("height") as? Number)?.toInt() ?: 48).dpToPx(context)
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

    private fun createButtonView(context: Context, props: Map<String, Any>?): View {
        return com.example.mylibrary.ds.button.DsButton(context).apply {
            val type = props?.get("buttonType")?.toString() ?: "primary"

            setButtonType(when(type) {
                "secondary" -> DsButton.ButtonType.SECONDARY
                "outlined" -> DsButton.ButtonType.OUTLINED
                "danger" -> DsButton.ButtonType.DANGER
                else -> DsButton.ButtonType.PRIMARY
            })
            setDsText(props?.get("text")?.toString() ?: "")
        }
    }
    private fun createProgressBar(context: Context, props: Map<String, Any>?): View {
        return android.widget.ProgressBar(context).apply {
            isIndeterminate = props?.get("isIndeterminate") as? Boolean ?: true

            val colorHex = props?.get("color")?.toString() ?: "#0E64D2"
            try {
                val color = Color.parseColor(colorHex)
                this.indeterminateTintList = android.content.res.ColorStateList.valueOf(color)
            } catch (e: Exception) {
                android.util.Log.e("DS_DEBUG", "Cor de ProgressBar inválida: $colorHex")
            }

            layoutParams = LinearLayout.LayoutParams(
                WRAP_CONTENT,
                WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER

                if (props?.get("fullScreen") == true) {
                    weight = 1f
                    height = MATCH_PARENT
                }
            }
        }
    }
    private fun createHeaderView(context: Context, props: Map<String, Any>?): View {
        return com.example.mylibrary.ds.toolbar.DsToolbar(context).apply {
            setToolbarTitle(
                titleText = props?.get("title")?.toString() ?: "",
                textStyle = DsText.TextStyle.HEADER
            )
        }
    }

    private fun createNewsCard(context: Context, props: Map<String, Any>?): View {
        return com.example.mylibrary.ds.card.news.DsNewsCard(context).apply {
            val title = props?.get("title")?.toString() ?: ""
            val description = props?.get("description")?.toString() ?: ""
            val time = props?.get("date")?.toString() ?: ""
            val imageUrl = props?.get("imageUrl")?.toString()

            setNews(
                title = title,
                description = description,
                time = time,
                imageLoader = {
                    imageUrl?.let { url ->
                        this.load(url) {
                            crossfade(true)
                            placeholder(android.R.drawable.ic_menu_report_image)
                            error(android.R.drawable.stat_notify_error)
                        }
                    }
                }
            )
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

    private fun createNotificationCard(context: Context, props: Map<String, Any>?): View {
        return com.example.mylibrary.ds.card.notification.DsNotificationCard(context).apply {

            val title = props?.get("title")?.toString() ?: ""
            val dateTime = props?.get("date")?.toString() ?: props?.get("dateTime")?.toString() ?: ""
            val isNew = props?.get("isNew") as? Boolean ?: false

            setTitle(title)
            setDateTime(dateTime)
            setIsNew(isNew)

            props?.get("chipText")?.toString()?.let { setChipText(it) }

            props?.get("chipBgColor")?.toString()?.let {
                setChipBackgroundColor(android.graphics.Color.parseColor(it))
            }

            props?.get("chipTextColor")?.toString()?.let {
                setChipTextColor(android.graphics.Color.parseColor(it))
            }
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
            ((props?.get("margin_left") as? Number)?.toInt() ?: 0).dpToPx(context),
            ((props?.get("margin_top") as? Number)?.toInt() ?: 0).dpToPx(context),
            ((props?.get("margin_right") as? Number)?.toInt() ?: 0).dpToPx(context),
            ((props?.get("margin_bottom") as? Number)?.toInt() ?: 0).dpToPx(context)
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