package com.domleondev.designsystem.runtime

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
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
internal class  DesignSystemImpl @Inject constructor(
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

    private var submitButton: DsButton? = null

    override fun createView(context: Context, component: Component): View? {


        val view = factory.createView(context, component) ?: return null

        val props = component.props ?: emptyMap()
        val id = component.id.orEmpty()
        val action = props["action"]?.toString()

        applyMargins(view, props, context)
        applyAlignment(view, props)
        applyFocusNavigation(view, props)
        applyAccessibility(view, props)

        when (view) {
            is DsInput -> {
                applyInputProps(view, props)
                inputRegistry[id] = view

                val rules = props.parseValidationRules()
                rulesRegistry[id] = rules


                view.doAfterTextChanged { text ->
                    val value = text?.toString().orEmpty()

                    eventsFlow.tryEmit(DsUiEvent.Change(id, value))


                    if (props.getBooleanSafe("validateOnChange") == true) {
                        val error = validateField(id, value, rules)

                        setError(id, error)
                    }
                    updateSubmitButtonState()
                }
            }
            is DsButton -> {
                applyTextProps(view, props, context)

                if (props["submit"] as? Boolean == true) {
                    submitButton = view
                    view.isEnabled = false
                    view.alpha = 0.5f
                }
                view.setOnClickListener {
                    if (props["submit"] as? Boolean == true) {
                        handleSubmitAction(id, props)
                    } else {
                        eventsFlow.tryEmit(DsUiEvent.Action(id, props["action"]?.toString() ?: ""))
                    }
                }
            }
            is androidx.constraintlayout.widget.ConstraintLayout -> {
                if (component.type == "Header") {
                    val iconButton = view.getChildAt(0)

                    val isMenu = props["showMenu"] as? Boolean ?: false
                    val defaultAction = if (isMenu) "menu:open" else "navigate:back"
                    val finalAction = props["action"]?.toString() ?: defaultAction

                    iconButton?.setOnClickListener {
                        android.util.Log.d("DS_DEBUG", "Header clicado! Enviando action: $finalAction")
                        eventsFlow.tryEmit(DsUiEvent.Action(id, finalAction))
                    }
                }
            }
            is LinearLayout -> {
                if (component.type == "MenuItem") {
                    view.setOnClickListener {
                        val action = props["action"]?.toString()
                        action?.let { eventsFlow.tryEmit(DsUiEvent.Action(id, it)) }
                    }
                }
            }
            is TextView -> {
                applyTextProps(view, props, context)
            }
        }
        if (id.isNotEmpty()) {
            viewRegistry[id] = view
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
                updateInputBorderStyle(input, "#FF0000")
            } else {
                clearErrorA11y(input)
                updateInputBorderStyle(input, "#CCCCCC")
            }
        }
    }
    private fun updateInputBorderStyle(view: View, color: String) {
        val shape = view.background as? GradientDrawable ?: GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 5.dpToPx(view.context).toFloat()
        }

        try {
            val strokeWidth = 2.dpToPx(view.context)
            shape.setStroke(strokeWidth, Color.parseColor(color))
            view.background = shape
            view.invalidate()
        } catch (e: Exception) {
            android.util.Log.e("DS_DEBUG", "❌ Erro visual: ${e.message}")
        }
    }
    private fun updateSubmitButtonState() {
        var isFormValid = true


        inputRegistry.keys.forEach { inputId ->
            val input = inputRegistry[inputId]
            val rules = rulesRegistry[inputId].orEmpty()
            val value = input?.text?.toString().orEmpty()


            if (validateField(inputId, value, rules) != null) {
                isFormValid = false
            }
        }

        submitButton?.let { button ->
            button.isEnabled = isFormValid
            button.alpha = if (isFormValid) 1.0f else 0.5f
        }
    }
    private fun handleSubmitAction(id: String, props: Map<String, Any?>) {
        when (val result =  validate()) {
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

                "cpf" -> {
                    if (!isValidCpf(value)) {
                        return rule.message.ifBlank { "CPF inválido" }
                    }
                }

                "match" -> {
                    val targetId = rule.params["targetId"]?.toString() ?: ""
                    val targetValue = getValue(targetId).orEmpty()
                    if (value != targetValue) {
                        return rule.message.ifBlank { "Os campos não coincidem" }
                    }
                }

                "regex" -> {
                    val patternString = rule.params["pattern"]?.toString() ?: continue
                    val pattern = patternString.toRegex(RegexOption.IGNORE_CASE)
                    if (!pattern.matches(value)) {
                        return rule.message.ifBlank { "Formato inválido" }
                    }
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

    private fun isValidCpf(cpf: String): Boolean {
        val cleanCpf = cpf.replace(Regex("[^0-9]"), "")
        if (cleanCpf.length != 11 || cleanCpf.all { it == cleanCpf[0] }) return false

        fun calculateDigit(subset: String, weights: IntArray): Int {
            val sum = subset.mapIndexed { i, c -> Character.getNumericValue(c) * weights[i] }.sum()
            val remainder = sum % 11
            return if (remainder < 2) 0 else 11 - remainder
        }

        val digit1 = calculateDigit(cleanCpf.substring(0, 9), intArrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2))
        val digit2 = calculateDigit(cleanCpf.substring(0, 10), intArrayOf(11, 10, 9, 8, 7, 6, 5, 4, 3, 2))

        return cleanCpf[9].digitToInt() == digit1 && cleanCpf[10].digitToInt() == digit2
    }

    private fun announceGlobalErrorIfNeeded(firstMessage: String?) {

    }

    private fun applyMargins(view: View, props: Map<String, Any?>, context: Context) {
        val lp = view.layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        fun Int.dp() = (this * context.resources.displayMetrics.density).toInt()
        lp.setMargins(
            (props.getNumberAsInt("margin_left") ?: 0).dp(),
            (props.getNumberAsInt("margin_top") ?: 0).dp(),
            (props.getNumberAsInt("margin_right") ?: 0).dp(),
            (props.getNumberAsInt("margin_bottom") ?: 0).dp()
        )
        val gravityProp = props["gravity"]?.toString()?.lowercase()
        if (gravityProp == "center") {
            lp.gravity = android.view.Gravity.CENTER_HORIZONTAL
        }
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

    override fun clear() {
        inputRegistry.clear()
        rulesRegistry.clear()
        viewRegistry.clear()
        submitButton = null
        android.util.Log.d("DS_DEBUG", "🧹 Motor limpo para a nova tela")
    }

}

private fun applyTextProps(view: TextView, props: Map<String, Any?>, context: Context) {
    val fontName = props["typeface"]?.toString()
    if (fontName != null) {
        val fontResId = context.resources.getIdentifier(fontName, "font", context.packageName)
        if (fontResId != 0) {
            view.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, fontResId)
        }
    }

    val spans = props["spans"] as? List<Map<String, String>>
    if (!spans.isNullOrEmpty()) {
        val parts = spans.map { span ->
            val text = span["text"].orEmpty()
            val color = android.graphics.Color.parseColor(span["color"] ?: "#000000")
            text to color
        }.toTypedArray()
        view.setColoredText(*parts)
    } else {
        view.text = props["title"]?.toString().orEmpty()
    }
}

private fun applyInputProps(view: DsInput, props: Map<String, Any?>) {
    val keyboardType = props.getString("keyboardType")?.lowercase()
    val textInputLayout = view as? com.google.android.material.textfield.TextInputLayout
        ?: view.parent as? com.google.android.material.textfield.TextInputLayout
    when (keyboardType) {
        "password" -> {
            view.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            textInputLayout?.let { layout ->
                layout.endIconMode =
                    com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
            }
            view.setSelection(view.text?.length ?: 0)
        }

        "email" -> {
            view.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
    }
}

private fun TextView.setColoredText(vararg parts: Pair<String, Int>) {
    val builder = android.text.SpannableStringBuilder()
    for ((text, color) in parts) {
        val start = builder.length
        builder.append(text)
        builder.setSpan(
            android.text.style.ForegroundColorSpan(color),
            start,
            builder.length,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    this.text = builder
}


private fun Map<String, Any?>.getString(key: String): String? =
    this[key]?.toString()

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
private fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}