package com.domleondev.designsystem.domain.repository

interface RemoteConfigRepository {
    fun fetchScreenConfig(key: String, onComplete: (String) -> Unit)
}