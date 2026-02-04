package com.domleondev.layerdesignsystem

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.domleondev.designsystem.domain.renderer.UiRenderer
import com.domleondev.designsystem.presentation.state.UiState
import com.domleondev.layerdesignsystem.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.hilt.android.AndroidEntryPoint
import java.util.Objects.toString
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var uiRenderer: UiRenderer

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.loadScreen("forgotPassword")
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    uiRenderer.render(binding.containerLayout, state.screen)
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
}

