package com.android.daviddev.ecoscancmem.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.daviddev.ecoscancmem.R
import com.android.daviddev.ecoscancmem.data.model.EcopointColor
import com.android.daviddev.ecoscancmem.data.model.ScanResult
import com.android.daviddev.ecoscancmem.databinding.FragmentResultBinding
import com.android.daviddev.ecoscancmem.databinding.ItemTipRowBinding
import com.android.daviddev.ecoscancmem.viewmodel.ScanViewModel

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!
    private val args: ResultFragmentArgs by navArgs()
    private val scanViewModel: ScanViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val result = args.scanResult
        bindHero(result)
        bindEcopoints(result)
        bindImpact(result)
        bindOcr(result)
        bindTips(result)
        bindButtons(result)
    }

    private fun bindHero(r: ScanResult) {
        binding.tvMaterialName.text = r.materialName
        binding.tvMaterialSub.text = r.materialSubtitle
        binding.confidenceBar.progress = r.confidencePercent
        binding.tvConfidence.text = "${r.confidencePercent}% confiança"
        binding.ivMaterialIcon.setImageResource(iconForMaterial(r.recycleCode))
    }

    private fun bindEcopoints(r: ScanResult) {
        val (name, icon, bg) = ecopointInfo(r.ecopointColor)
        binding.tvEcopointName.text = name
        binding.tvEcopointSub.text =
            "${getString(R.string.ecopoint_accepts)} · ${getString(R.string.ecopoint_nearby)}"
        binding.ivEcopointIcon.setImageResource(icon)
        binding.ivEcopointIcon.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bg))

        binding.cardEcopointMain.setOnClickListener {
            //findNavController().navigate(R.id.action_result_to_map)
        }
    }

    private fun bindImpact(r: ScanResult) {
        binding.tvCo2Value.text = if (r.co2SavedGrams > 0) "−${r.co2SavedGrams} g" else "—"
        binding.tvDecompValue.text = if (r.decompYears > 0) "${r.decompYears} anos" else "—"
        binding.tvEnergyValue.text =
            if (r.energySavedPercent > 0) "${r.energySavedPercent}%" else "—"
        binding.tvRecycleCode.text = r.recycleCode
    }

    private fun bindOcr(r: ScanResult) {
        val hasOcr = !r.ocrRawText.isNullOrBlank()
        binding.labelOcr.isVisible = hasOcr
        binding.cardOcr.isVisible = hasOcr
        if (hasOcr) {
            binding.tvOcrRaw.text = "\"${r.ocrRawText}\""
            binding.tvOcrDecoded.text = r.ocrDecodedText
        }
    }

    private fun bindTips(r: ScanResult) {
        binding.tipsContainer.removeAllViews()
        r.tips.forEach { tip ->
            val row = ItemTipRowBinding.inflate(layoutInflater, binding.tipsContainer, true)
            row.tvTip.text = tip
        }
    }

    private fun bindButtons(r: ScanResult) {
        binding.btnSave.setOnClickListener {
            // Guarda na Room DB via ViewModel
            scanViewModel.saveCurrentResult()
            Toast.makeText(requireContext(), getString(R.string.saved_ok), Toast.LENGTH_SHORT)
                .show()
            findNavController().navigate(R.id.action_result_to_history)
        }
        binding.btnNewScan.setOnClickListener {
            scanViewModel.clearResult()
            findNavController().popBackStack(R.id.scanFragment, false)
            //findNavController().navigateUp()
            //findNavController().navigate(R.id.action_result_to_scan)
        }
        binding.btnBack.setOnClickListener {
            scanViewModel.clearResult()
            findNavController().popBackStack(R.id.scanFragment, false)
            //findNavController().navigateUp()
        }
        binding.btnShare.setOnClickListener {
            shareResult(r)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun shareResult(r: ScanResult) {
        val text = """
            EcoScan — ${r.materialName}
            Destino: ${ecopointInfo(r.ecopointColor).first}
            CO2 poupado: ${r.co2SavedGrams} g
            Código: ${r.recycleCode}
        """.trimIndent()
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }, getString(R.string.share_title)
            )
        )
    }

    private fun iconForMaterial(code: String) = when {
        code.contains("PET", ignoreCase = true) -> R.drawable.ic_camera_scan // Fallback
        code.contains("GL", ignoreCase = true) -> R.drawable.ic_camera_scan // Fallback
        code.contains("PAP", ignoreCase = true) -> R.drawable.ic_camera_scan // Fallback
        code.contains("ALU", ignoreCase = true) -> R.drawable.ic_camera_scan // Fallback
        else -> R.drawable.ic_camera_scan // Fallback
    }

    private fun ecopointInfo(color: EcopointColor) = when (color) {
        EcopointColor.YELLOW -> Triple(
            getString(R.string.ecopoint_yellow),
            R.drawable.ic_ecopoint,
            R.color.teal_bg
        )

        EcopointColor.BLUE -> Triple(
            getString(R.string.ecopoint_blue),
            R.drawable.ic_ecopoint,
            R.color.blue_bg
        )

        EcopointColor.GREEN -> Triple(
            getString(R.string.ecopoint_green),
            R.drawable.ic_ecopoint,
            R.color.green_bg
        )

        EcopointColor.RED -> Triple(
            getString(R.string.ecopoint_red),
            R.drawable.ic_ecopoint,
            R.color.amber_bg
        )

        EcopointColor.NONE -> Triple(
            getString(R.string.ecopoint_none),
            R.drawable.ic_ecopoint,
            R.color.sensor_label
        )
    }
}