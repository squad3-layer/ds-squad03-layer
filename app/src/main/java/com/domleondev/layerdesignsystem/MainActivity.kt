package com.domleondev.layerdesignsystem

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.domleondev.designsystem.contract.DesignSystem
import com.domleondev.designsystem.contract.DsUiEvent
import com.domleondev.designsystem.domain.renderer.UiRenderer
import com.domleondev.designsystem.presentation.state.UiState
import com.domleondev.layerdesignsystem.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Objects.toString
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var uiRenderer: UiRenderer
    @Inject lateinit var designSystem: DesignSystem

    private lateinit var firebaseAnalytics: com.google.firebase.analytics.FirebaseAnalytics
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAnalytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(this)

        viewModel.loadScreen("reset_password")
        observeViewModel()
        observeDesignSystemEvents()

        binding.containerMenu.setOnClickListener {
            binding.containerMenu.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.containerLayout.removeAllViews()
                    renderLoading()
                }
                is UiState.Success -> {


                    val pageComponents = state.screen.components.filter { it.type != "MenuItem" }
                    uiRenderer.render(binding.containerLayout, state.screen.copy(components = pageComponents))

                    val menuComponents = state.screen.components.filter { it.type == "MenuItem" }
                    if (menuComponents.isNotEmpty()) {
                        uiRenderer.render(binding.menuContent, state.screen.copy(components = menuComponents))
                    }
                }
                is UiState.Error -> {
                    binding.containerLayout.removeAllViews()
                    renderLoading()
                }
            }
        }
    }

    private fun renderLoading() {
        val loadingDefinition = com.domleondev.designsystem.domain.model.ScreenDefinition(
            screenName = "loading_state",
            components = listOf(
                com.domleondev.designsystem.domain.model.Component(
                    type = "ProgressBar",
                    props = mapOf("fullScreen" to true, "color" to "#0E64D2")
                )
            )
        )
        uiRenderer.render(binding.containerLayout, loadingDefinition)
    }
    private fun observeDesignSystemEvents() {
        lifecycleScope.launch {
            designSystem.eventStream().events.collect { event ->
                when (event) {
                    is DsUiEvent.Analytics -> {
                        android.util.Log.d("APP_EVENT", "Analytics disparado: ${event.eventName}")
                        val bundle = Bundle().apply {
                            putString("origin", "server_driven_ui")
                        }
                        firebaseAnalytics.logEvent(event.eventName, bundle)
                    }
                    is DsUiEvent.Action -> {
                        android.util.Log.d("APP_EVENT", "Action recebida: ${event.action}")
                        if (event.action == "menu:open") {
                            showSideMenu()
                        }
                        handleNavigation(event.action)
                    }
                    is DsUiEvent.Submit -> {
                        android.util.Log.d("APP_EVENT", "Submit da tela: ${event.screenId}")
                    }

                    else -> {}
                }
            }
        }
    }
    private fun showSideMenu() {
        binding.containerMenu.visibility = View.VISIBLE
    }

    private fun handleNavigation(action: String) {
        if (action.startsWith("navigate:")) {
            val destination = action.substringAfter(":")

            when (destination) {
                "login" -> {
                    android.util.Log.d("APP_EVENT", "Sucesso! Saindo da tela...")
                    Toast.makeText(this, "Navegando para o Login...", Toast.LENGTH_SHORT).show()
                }
                "back" -> {
                    finish()
                }
            }
        }
    }
}

