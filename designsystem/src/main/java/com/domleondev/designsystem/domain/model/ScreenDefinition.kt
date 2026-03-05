package com.domleondev.designsystem.domain.model

import com.google.gson.annotations.SerializedName

data class ScreenDefinition(
    @SerializedName("screen")
    val screenName: String,

    @SerializedName("components")
    val components: List<Component>
)