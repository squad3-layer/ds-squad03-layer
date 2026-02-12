package com.domleondev.designsystem.runtime

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.domleondev.designsystem.ComponentFactory
import com.domleondev.designsystem.contract.*
import com.example.mylibrary.ds.button.DsButton
import com.example.mylibrary.ds.input.DsInput
import com.domleondev.designsystem.domain.model.Component
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DesignSystemImpl @Inject constructor(
    private val factory: ComponentFactory
) : DesignSystem {

    private val eventsFlow = MutableSharedFlow<DsUiEvent>(extraBufferCapacity = 64)
    private val _eventStream = object : DsEventStream {
        override val events = eventsFlow.asSharedFlow()
    }
    private val inputRegistry = linkedMapOf<String, DsInput>()
    private val rulesRegistry = linkedMapOf<String, List<DsValidationRule>>()

    override fun eventStream(): DsEventStream = _eventStream
    private val viewRegistry = linkedMapOf<String, View>()

    override fun createView(context: Context, component: Component): View? {

        val view = factory.createView(context, component) ?: return null

        val props = component.props ?: emptyMap()
        val id = component.id.orEmpty()
        val action = props["action"]?.toString()


        if (id.isNotEmpty()) {
            viewRegistry[id] = view
        }


        if (view is DsInput) {
            inputRegistry[id] = view
            rulesRegistry[id] = props.parseValidationRules()


            view.doAfterTextChanged { text ->
                eventsFlow.tryEmit(DsUiEvent.Change(id, text?.toString().orEmpty()))
            }
        }

        if (action != null) {
            view.setOnClickListener {

                eventsFlow.tryEmit(DsUiEvent.Action(id, action))


                if (props["submit"] as? Boolean == true) {
                    handleSubmitAction(id, props)
                }
            }
        }

        return view
    }

    override fun setEnabled(id: String, enabled: Boolean) {
        viewRegistry[id]?.let { view ->
            view.isEnabled = enabled
            view.alpha = if (enabled) 1.0f else 0.5f
        }
    }
    override fun getValue(id: String): String? {
        return inputRegistry[id]?.text?.toString()
    }
    override fun setError(id: String, message: String?) {
        inputRegistry[id]?.let { input ->
            if (message != null) {
                showErrorA11y(input, message)
            } else {
                clearErrorA11y(input)
            }
        }
    }

    private fun handleSubmitAction(id: String, props: Map<String, Any?>) {
        when (val result = validate()) {
            is DsValidationResult.Valid -> {
                eventsFlow.tryEmit(DsUiEvent.Submit(id))
            }
            is DsValidationResult.Invalid -> {
                announceGlobalErrorIfNeeded(result.errors.values.firstOrNull())
            }
        }
    }

override fun validate(vararg fieldIds: String): DsValidationResult {
    val targets = if (fieldIds.isEmpty()) inputRegistry.keys else fieldIds.toList()
    val errors = mutableMapOf<String, String>()
    targets.forEach { id ->
        val input = inputRegistry[id] ?: return@forEach
        val rules = rulesRegistry[id].orEmpty()
        val value = input.text?.toString().orEmpty()
        validateField(id, value, rules)?.let { msg ->
            errors[id] = msg
            showErrorA11y(input, msg)
        } ?: clearErrorA11y(input)
    }
    return if (errors.isEmpty()) DsValidationResult.Valid else DsValidationResult.Invalid(errors)
}

override fun clearValidation(vararg fieldIds: String) {
    val targets =
        if (fieldIds.isEmpty()) inputRegistry.values else fieldIds.mapNotNull { inputRegistry[it] }
    targets.forEach { clearErrorA11y(it) }
}


private fun createTextView(context: Context, props: Map<String, Any?>): TextView =
    TextView(context).apply {
        text = props.getString("title").orEmpty()
        textSize = props.getNumberAsFloat("size") ?: 16f
        when (props.getString("weight")?.lowercase()) {
            "bold" -> setTypeface(typeface, Typeface.BOLD)
            "italic" -> setTypeface(typeface, Typeface.ITALIC)
            else -> setTypeface(typeface, Typeface.NORMAL)
        }
    }

private fun createInputView(context: Context, props: Map<String, Any?>, id: String): DsInput =
    DsInput(context).apply {
        this.hint = props.getString("hint").orEmpty()
        inputRegistry[id] = this
        rulesRegistry[id] = props.parseValidationRules()


        doAfterTextChanged { text ->
            val value = text?.toString().orEmpty()
            eventsFlow.tryEmit(DsUiEvent.Change(id, value))

            if (props.getBooleanSafe("validateOnChange") == true) {

                val rule = validateField(id, value, rulesRegistry[id].orEmpty())
                if (rule == null) clearErrorA11y(this) else showErrorA11y(this, rule)
            }
        }
    }

private fun createButtonView(context: Context, props: Map<String, Any?>, id: String): DsButton =
    DsButton(context).apply {
        text = props.getString("text").orEmpty()
        isFocusable = true

        setOnClickListener {

            props.getString("analyticsEvent")?.let { eventName ->
                eventsFlow.tryEmit(DsUiEvent.Analytics(eventName))
            }

            eventsFlow.tryEmit(DsUiEvent.Click(id))

            props.getString("action")?.let { actionValue ->
                eventsFlow.tryEmit(DsUiEvent.Action(id, actionValue))
            }

            val isSubmit = props.getBooleanSafe("submit") == true
            if (isSubmit) {
                when (val result = validate()) {
                    is DsValidationResult.Valid -> {
                        val screenId = props.getString("screenId") ?: "unknown"
                        eventsFlow.tryEmit(DsUiEvent.Submit(screenId))
                    }

                    is DsValidationResult.Invalid -> {
                        announceGlobalErrorIfNeeded(result.errors.values.firstOrNull())
                    }
                }
            }
        }
    }


private fun validateField(
    id: String,
    value: String,
    rules: List<DsValidationRule>
): String? {
    for (rule in rules) {
        when (rule.type.lowercase()) {
            "required" -> if (value.isBlank()) return rule.message.ifBlank { "Campo obrigatório" }
            "email" -> if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches())
                return rule.message.ifBlank { "E-mail inválido" }

            "minlength" -> {
                val min = (rule.params["min"] as? Number)?.toInt() ?: 0
                if (value.length < min) return rule.message.ifBlank { "Mínimo de $min caracteres" }
            }

            "regex" -> {
                val pattern = rule.params["pattern"]?.toString()?.toRegex() ?: continue
                if (!pattern.matches(value)) return rule.message.ifBlank { "Formato inválido" }
            }
        }
    }
    return null
}


private fun showErrorA11y(input: DsInput, message: String) {

    try {
        input.error = message
    } catch (_: Throwable) {
    }
    ViewCompat.setStateDescription(input, message)
    ViewCompat.setAccessibilityLiveRegion(input, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
    input.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT)
}

private fun clearErrorA11y(input: DsInput) {
    try {
        input.error = null
    } catch (_: Throwable) {
    }
    ViewCompat.setStateDescription(input, null)
    ViewCompat.setAccessibilityLiveRegion(input, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE)
}

private fun announceGlobalErrorIfNeeded(firstMessage: String?) {

}

private fun applyMargins(view: View, props: Map<String, Any?>, context: Context) {
    val lp = (view.layoutParams as? ViewGroup.MarginLayoutParams)
        ?: ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    fun Int.dp() = (this * context.resources.displayMetrics.density).toInt()
    lp.setMargins(
        (props.getNumberAsInt("margin_left") ?: 0).dp(),
        (props.getNumberAsInt("margin_top") ?: 0).dp(),
        (props.getNumberAsInt("margin_right") ?: 0).dp(),
        (props.getNumberAsInt("margin_bottom") ?: 0).dp()
    )
    view.layoutParams = lp
}

private fun applyAlignment(view: View, props: Map<String, Any?>) {
    val align = props.getString("align")?.lowercase()
    if (view is TextView) {
        when (align) {
            "center" -> {
                view.textAlignment = View.TEXT_ALIGNMENT_CENTER
                view.gravity = android.view.Gravity.CENTER_HORIZONTAL
            }

            "left" -> {
                view.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                view.gravity = android.view.Gravity.START
            }

            "right" -> {
                view.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
                view.gravity = android.view.Gravity.END
            }
        }
    }
}

private fun applyFocusNavigation(view: View, props: Map<String, Any?>) {
    props.getNumberAsInt("next_focus_up")?.let { view.nextFocusUpId = it }
    props.getNumberAsInt("next_focus_down")?.let { view.nextFocusDownId = it }
    props.getNumberAsInt("next_focus_left")?.let { view.nextFocusLeftId = it }
    props.getNumberAsInt("next_focus_right")?.let { view.nextFocusRightId = it }
}

private fun applyAccessibility(view: View, props: Map<String, Any?>) {

    when (props.getString("importantForAccessibility")?.lowercase()) {
        "yes" -> view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        "no" -> view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        "auto", null -> {}
    }


    props.getString("accessibilityLabel")?.takeIf { it.isNotBlank() }?.let { label ->

        if (view !is TextView || view.text.isNullOrBlank()) view.contentDescription = label
    }
    props.getString("accessibilityHint")?.takeIf { it.isNotBlank() }?.let { hint ->
        ViewCompat.replaceAccessibilityAction(
            view,
            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
            hint
        ) { _, _ -> false }
        try {
            view.tooltipText = hint
        } catch (_: Throwable) {
        }
    }

    if (props.getBooleanSafe("isHeading") == true) {
        ViewCompat.setAccessibilityHeading(view, true)
    }
}
}

private fun applyVisualProps(view: View, props: Map<String, Any?>, context: Context) {

    val bgColor = props.getString("backgroundColor")
    val borderRadius = props.getNumberAsFloat("border_radius") ?: 0f
    val borderColor = props.getString("border_color")

    if (bgColor != null || borderRadius > 0f || borderColor != null) {
        val shape = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE

            bgColor?.let { setColor(android.graphics.Color.parseColor(it)) }

            borderColor?.let {
                setStroke(2.dpToPx(context).toInt(), android.graphics.Color.parseColor(it))
            }
            cornerRadius = borderRadius.dpToPx(context)
        }
        view.background = shape
    }


    if (view is TextView) {
        props.getString("textColor")?.let {
            view.setTextColor(android.graphics.Color.parseColor(it))
        }
    }


    view.visibility = if (props.getString("visibility") == "hidden") View.GONE else View.VISIBLE


    val pLeft = (props.getNumberAsInt("padding_left") ?: 0).dpToPx(context).toInt()
    val pTop = (props.getNumberAsInt("padding_top") ?: 0).dpToPx(context).toInt()
    val pRight = (props.getNumberAsInt("padding_right") ?: 0).dpToPx(context).toInt()
    val pBottom = (props.getNumberAsInt("padding_bottom") ?: 0).dpToPx(context).toInt()
    view.setPadding(pLeft, pTop, pRight, pBottom)
}


private fun Float.dpToPx(context: Context): Float =
    this * context.resources.displayMetrics.density

private fun Int.dpToPx(context: Context): Float =
    this.toFloat() * context.resources.displayMetrics.density


private fun Map<String, Any?>.getString(key: String): String? =
    this[key]?.toString()

private fun Map<String, Any?>.getNumberAsFloat(key: String): Float? = when (val v = this[key]) {
    is Number -> v.toFloat()
    is String -> v.toFloatOrNull()
    else -> null
}

private fun Map<String, Any?>.getNumberAsInt(key: String): Int? = when (val v = this[key]) {
    is Number -> v.toInt()
    is String -> v.toIntOrNull()
    else -> null
}

private fun Map<String, Any?>.getBooleanSafe(key: String): Boolean? = when (val v = this[key]) {
    is Boolean -> v
    is Number -> v.toInt() != 0
    is String -> v.equals("true", ignoreCase = true)
    else -> null
}

private fun Map<String, Any?>.parseValidationRules(): List<DsValidationRule> {
    val raw = this["rules"] as? List<*> ?: return emptyList()
    return raw.mapNotNull { any ->
        (any as? Map<*, *>)?.let { mm ->
            DsValidationRule(
                type = mm["component"]?.toString().orEmpty(),
                params = (mm["params"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                    (k as? String)?.let { it to v }
                }?.toMap() ?: emptyMap(),
                message = mm["message"]?.toString().orEmpty()
            )
        }
    }
}