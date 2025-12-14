package com.edgeai.vox.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.edgeai.vox.R
import com.edgeai.vox.databinding.ActivityMainBinding
import com.edgeai.vox.pipeline.VoicePipeline
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onMicPermissionGranted()
        } else {
            Toast.makeText(this, R.string.permission_audio_rationale, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.inputBroker.setText(viewModel.defaultBrokerUri)
        setupListeners()
        observeState()
        viewModel.initializeEngines()
    }

    private fun setupListeners() {
        binding.btnConnect.setOnClickListener {
            val broker = binding.inputBroker.text?.toString().orEmpty()
            val clientId = binding.inputClientId.text?.toString()
            viewModel.connectMqtt(broker, clientId)
        }

        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnectMqtt()
        }

        binding.btnMic.setOnClickListener {
            if (viewModel.isProcessing.value) return@setOnClickListener
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED -> onMicPermissionGranted()
                else -> requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun onMicPermissionGranted() {
        lifecycleScope.launch {
            viewModel.runVoicePipeline()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pipelineStage.collect { stage ->
                        binding.textPipelineStatus.text = stageLabel(stage)
                        binding.btnMic.isEnabled = stage != VoicePipeline.Stage.LISTENING
                    }
                }
                launch {
                    viewModel.transcript.collect { binding.textTranscript.text = it }
                }
                launch {
                    viewModel.llmOutput.collect { binding.textLlmOutput.text = it }
                }
                launch {
                    viewModel.mqttConnected.collect { connected ->
                        binding.textMqttStatus.text = if (connected) {
                            getString(R.string.status_connected)
                        } else {
                            getString(R.string.status_disconnected)
                        }
                    }
                }
                launch {
                    viewModel.combinedLogs.collect { logs ->
                        binding.textLogs.text = logs.joinToString("\n")
                        binding.scrollLogs.post {
                            binding.scrollLogs.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
                        }
                    }
                }
                launch {
                    viewModel.lastError.collect { error ->
                        error?.let { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() }
                    }
                }
            }
        }
    }

    private fun stageLabel(stage: VoicePipeline.Stage): String = when (stage) {
        VoicePipeline.Stage.IDLE -> getString(R.string.status_idle)
        VoicePipeline.Stage.LISTENING -> getString(R.string.status_listening)
        VoicePipeline.Stage.TRANSCRIBING -> getString(R.string.status_transcribing)
        VoicePipeline.Stage.INTERPRETING -> getString(R.string.status_interpreting)
        VoicePipeline.Stage.PUBLISHING -> getString(R.string.status_publishing)
        VoicePipeline.Stage.ERROR -> getString(R.string.status_idle)
    }
}
