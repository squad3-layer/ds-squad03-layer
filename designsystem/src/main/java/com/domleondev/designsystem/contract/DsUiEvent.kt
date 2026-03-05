package com.domleondev.designsystem.contract

sealed class DsUiEvent {
    data class Click(val componentId: String) : DsUiEvent()
    data class Change(val componentId: String, val value: String) : DsUiEvent()
    data class Submit(val screenId: String) : DsUiEvent()

    data class Analytics(val eventName: String) : DsUiEvent()
    data class Action(val componentId: String, val action: String) : DsUiEvent()
}

sealed class DsValidationResult {
    data object Valid : DsValidationResult()
    data class Invalid(val errors: Map<String, String>) : DsValidationResult()
}


data class DsValidationRule(
    val type: String,
    val params: Map<String, Any?> = emptyMap(),
    val message: String = ""
)


interface DsEventStream {

    val events: kotlinx.coroutines.flow.Flow<DsUiEvent>
}


interface DesignSystem {

    fun createView(context: android.content.Context, component: com.domleondev.designsystem.domain.model.Component): android.view.View?
    fun validate(vararg fieldIds: String): DsValidationResult
    fun eventStream(): DsEventStream
    fun clearValidation(vararg fieldIds: String)
    fun setEnabled(id: String, enabled: Boolean)
    fun getValue(id: String): String?
    fun setError(id: String, message: String?)
    fun clear()
}