package com.domleondev.designsystem.runtime

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.widget.doAfterTextChanged
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
    private val viewRegistry = linkedMapOf<String, View>()
    private var submitButton: DsButton? = null

    override fun eventStream(): DsEventStream = _eventStream

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
                        eventsFlow.tryEmit(DsUiEvent.Action(id, action ?: ""))
                    }
                }
            }
            is androidx.constraintlayout.widget.ConstraintLayout -> {
                if (component.type == "Header") {
                    val iconButton = view.getChildAt(0)
                    val isMenu = props["showMenu"] as? Boolean ?: false
                    val defaultAction = if (isMenu) "menu:open" else "navigate:back"
                    val clickAction = action ?: defaultAction

                    iconButton?.setOnClickListener {
                        eventsFlow.tryEmit(DsUiEvent.Action(id, clickAction))
                    }
                }
            }
            is LinearLayout -> {
                if (component.type == "MenuItem") {
                    view.setOnClickListener {
                        action?.let { eventsFlow.tryEmit(DsUiEvent.Action(id, it)) }
                    }
                }
            }
            is TextView -> {
                applyTextProps(view, props, context)
            }
        }

        if (id.isNotEmpty()) viewRegistry[id] = view
        return view
    }

    override fun setEnabled(id: String, enabled: Boolean) {
        viewRegistry[id]?.let { view ->
            view.isEnabled = enabled
            view.alpha = if (enabled) 1.0f else 0.5f
        }
    }

    override fun getValue(id: String): String? = inputRegistry[id]?.text?.toString()

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
            shape.setStroke(2.dpToPx(view.context), Color.parseColor(color))
            view.background = shape
        } catch (_: Exception) {}
    }

    private fun updateSubmitButtonState() {
        var isFormValid = true
        inputRegistry.keys.forEach { inputId ->
            val value = inputRegistry[inputId]?.text?.toString().orEmpty()
            if (validateField(inputId, value, rulesRegistry[inputId].orEmpty()) != null) {
                isFormValid = false
            }
        }
        submitButton?.let { button ->
            button.isEnabled = isFormValid
            button.alpha = if (isFormValid) 1.0f else 0.5f
        }
    }

    private fun handleSubmitAction(id: String, props: Map<String, Any?>) {
        if (validate() is DsValidationResult.Valid) {
            eventsFlow.tryEmit(DsUiEvent.Submit(id))
        }
    }

    override fun validate(vararg fieldIds: String): DsValidationResult {
        val targets = if (fieldIds.isEmpty()) inputRegistry.keys else fieldIds.toList()
        val errors = mutableMapOf<String, String>()
        targets.forEach { id ->
            val value = inputRegistry[id]?.text?.toString().orEmpty()
            validateField(id, value, rulesRegistry[id].orEmpty())?.let { msg ->
                errors[id] = msg
                showErrorA11y(inputRegistry[id]!!, msg)
            } ?: clearErrorA11y(inputRegistry[id]!!)
        }
        return if (errors.isEmpty()) DsValidationResult.Valid else DsValidationResult.Invalid(errors)
    }

    override fun clearValidation(vararg fieldIds: String) {
        val targets = if (fieldIds.isEmpty()) inputRegistry.values else fieldIds.mapNotNull { inputRegistry[it] }
        targets.forEach { clearErrorA11y(it) }
    }

    private fun validateField(id: String, value: String, rules: List<DsValidationRule>): String? {
        for (rule in rules) {
            when (rule.type.lowercase()) {
                "required" -> if (value.isBlank()) return rule.message.ifBlank { "Campo obrigatório" }
                "email" -> if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) return "E-mail inválido"
                "minlength" -> {
                    val min = (rule.params["min"] as? Number)?.toInt() ?: 0
                    if (value.length < min) return "Mínimo de $min caracteres"
                }
            }
        }
        return null
    }

    private fun showErrorA11y(input: DsInput, message: String) {
        input.error = message
        ViewCompat.setStateDescription(input, message)
        input.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT)
    }

    private fun clearErrorA11y(input: DsInput) {
        input.error = null
        ViewCompat.setStateDescription(input, null)
    }

    private fun applyMargins(view: View, props: Map<String, Any?>, context: Context) {
        val lp = view.layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(-1, -2)
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
        if (view is TextView) {
            val align = props.getString("align")?.lowercase()
            view.gravity = when (align) {
                "center" -> android.view.Gravity.CENTER_HORIZONTAL
                "right" -> android.view.Gravity.END
                else -> android.view.Gravity.START
            }
        }
    }

    private fun applyFocusNavigation(view: View, props: Map<String, Any?>) {
        props.getNumberAsInt("next_focus_down")?.let { view.nextFocusDownId = it }
    }

    private fun applyAccessibility(view: View, props: Map<String, Any?>) {
        props.getString("accessibilityLabel")?.let { view.contentDescription = it }
    }

    override fun clear() {
        inputRegistry.clear()
        rulesRegistry.clear()
        viewRegistry.clear()
        submitButton = null
    }
}

private fun applyTextProps(view: TextView, props: Map<String, Any?>, context: Context) {
    view.text = props["title"]?.toString().orEmpty()
}

private fun applyInputProps(view: DsInput, props: Map<String, Any?>) {
    if (props.getString("keyboardType") == "password") {
        view.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
    }
}

private fun Map<String, Any?>.getString(key: String): String? = this[key]?.toString()
private fun Map<String, Any?>.getNumberAsInt(key: String): Int? = (this[key] as? Number)?.toInt() ?: this[key]?.toString()?.toIntOrNull()
private fun Map<String, Any?>.getBooleanSafe(key: String): Boolean? = this[key] as? Boolean
private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
private fun Map<String, Any?>.parseValidationRules(): List<DsValidationRule> {
    val raw = this["rules"] as? List<*> ?: return emptyList()
    return raw.mapNotNull { any ->
        (any as? Map<*, *>)?.let { mm ->
            DsValidationRule(
                type = mm["component"]?.toString().orEmpty(),
                params = (mm["params"] as? Map<*, *>)?.mapNotNull { (k, v) -> (k as? String)?.let { it to v } }?.toMap() ?: emptyMap(),
                message = mm["message"]?.toString().orEmpty()
            )
        }
    }
}