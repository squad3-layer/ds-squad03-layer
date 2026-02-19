package com.domleondev.designsystem.domain.model

import com.google.gson.annotations.SerializedName

data class Component(
    @SerializedName("component")
    val type: String,

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("props")
    val props: Map<String, Any>? = null,

    @SerializedName("children")
    val children: List<Component>? = null
)