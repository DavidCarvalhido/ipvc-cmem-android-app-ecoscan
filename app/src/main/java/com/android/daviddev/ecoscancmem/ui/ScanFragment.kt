package com.android.daviddev.ecoscancmem.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.daviddev.ecoscancmem.R
import com.android.daviddev.ecoscancmem.camera.CameraManager
import com.android.daviddev.ecoscancmem.databinding.FragmentScanBinding
import com.android.daviddev.ecoscancmem.sensor.AccelerometerManager
import com.android.daviddev.ecoscancmem.sensor.LightSensorManager
import com.android.daviddev.ecoscancmem.viewmodel.ScanViewModel
import kotlinx.coroutines.launch

class ScanFragment : Fragment(R.layout.fragment_scan) {
    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ScanViewModel by viewModels()

    private lateinit var cameraManager: CameraManager
    private lateinit var lightSensor: LightSensorManager
    private lateinit var accelerometer: AccelerometerManager

    // Pedido de permissão
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                cameraManager.startCamera()
            } else {
                binding.tvHint.text = getString(R.string.camera_permission_denied)
                binding.btnCapture.isEnabled = false
            }
        }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    // Lifecycle
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSensors()
        setupCameraWithPermission()
        setupUi()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        lightSensor.start()
        accelerometer.start()
        viewModel.clearResult()
    }

    override fun onPause() {
        super.onPause()
        lightSensor.stop()
        accelerometer.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Sensors setup
    private fun setupSensors() {
        lightSensor = LightSensorManager(requireContext()).apply {
            onLuxChanged = { lux -> viewModel.onLuxChanged(lux) }
        }
        accelerometer = AccelerometerManager(requireContext()).apply {
            onReadingChanged = { x, y, z -> viewModel.onAccelerometerChanged(x, y, z) }
        }
    }

    private fun setupCameraWithPermission() {
        cameraManager = CameraManager(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            previewView = binding.previewView,
            onTextDetected = { text -> viewModel.processDetectedText(text) },
            onObjectDetected = { objects -> viewModel.processDetectedObjects(objects) }
        )

        if (hasCameraPermission()) {
            cameraManager.startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCapture.setOnClickListener {
            cameraManager.capturePhoto { uri ->
                viewModel.saveCapture(uri)
            }
        }

        binding.btnTorch.setOnClickListener {
            cameraManager.toggleTorch()
            binding.btnTorch.isSelected = !binding.btnTorch.isSelected
        }

        binding.tabObject.setOnClickListener {
            viewModel.setScanMode(ScanViewModel.ScanMode.OBJECT)
            updateTabUi(ScanViewModel.ScanMode.OBJECT)
        }

        binding.tabLabel.setOnClickListener {
            viewModel.setScanMode(ScanViewModel.ScanMode.LABEL)
            updateTabUi(ScanViewModel.ScanMode.LABEL)
        }
    }

    private fun updateTabUi(mode: ScanViewModel.ScanMode) {
        val isObject = mode == ScanViewModel.ScanMode.OBJECT
        binding.tabObject.setBackgroundResource(
            if (isObject) R.drawable.bg_tab_active else android.R.color.transparent
        )
        binding.tabLabel.setBackgroundResource(
            if (!isObject) R.drawable.bg_tab_active else android.R.color.transparent
        )
        binding.tabObject.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isObject) R.color.white else R.color.sensor_label
            )
        )
        binding.tabLabel.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (!isObject) R.color.white else R.color.sensor_label
            )
        )
    }

    // Observer
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Luz insuficiente
                launch {
                    viewModel.isLightSufficient.collect { sufficient ->
                        binding.lightWarning.isVisible = !sufficient
                    }
                }

                // Chip para o lux
                launch {
                    viewModel.luxLevel.collect { lux ->
                        binding.tvLuxValue.text = "${lux.toInt()} lux"
                        binding.tvLuxValue.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                if (lux >= 50f) R.color.green_primary else R.color.amber_warn
                            )
                        )
                    }
                }

                // Chip para a estabilidade
                launch {
                    viewModel.isDeviceStable.collect { stable ->
                        binding.tvStabilityValue.text =
                            if (stable) getString(R.string.stable) else getString(R.string.moving)
                        binding.tvStabilityValue.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                if (stable) R.color.green_primary else R.color.amber_warn
                            )
                        )
                    }
                }

                // Chip para mostrar se está modo ativo
                launch {
                    viewModel.activeModeLabel.collect { label ->
                        binding.tvModeValue.text = label
                    }
                }

                // Barra de análise
                launch {
                    viewModel.isAnalysing.collect { analysing ->
                        binding.analysisBar.isVisible = analysing
                    }
                }

                // Resultado pronto -> navega para o ResultFragment
                launch {
                    viewModel.analysisResult.collect { result ->
                        result ?: return@collect
                        val action = ScanFragmentDirections
                            .actionScanToResult(result)
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }
}