package com.domleondev.designsystem.data.repository

import com.domleondev.designsystem.domain.repository.RemoteConfigRepository
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import javax.inject.Inject

class FirebaseRemoteConfigRepository @Inject constructor() : RemoteConfigRepository {
    override fun fetchScreenConfig(key: String, onComplete: (String) -> Unit) {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate().addOnCompleteListener {
            val json = remoteConfig.getString(key)
            onComplete(json)
        }
    }
}