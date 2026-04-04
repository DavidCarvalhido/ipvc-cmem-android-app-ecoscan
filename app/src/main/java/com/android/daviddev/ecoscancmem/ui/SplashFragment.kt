package com.android.daviddev.ecoscancmem.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.daviddev.ecoscancmem.R
import com.android.daviddev.ecoscancmem.databinding.FragmentSplashBinding
import com.android.daviddev.ecoscancmem.databinding.ItemFeatureRowBinding

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val features = listOf(
        FeatureItem(R.drawable.ic_camera_scan, R.color.teal_bg,
            R.string.feature_scanner_title, R.string.feature_scanner_desc),
        FeatureItem(R.drawable.ic_text_ocr, R.color.green_bg,
            R.string.feature_ocr_title, R.string.feature_ocr_desc),
        FeatureItem(R.drawable.ic_recycle_dest, R.color.blue_bg,
            R.string.feature_dest_title, R.string.feature_dest_desc),
        FeatureItem(R.drawable.ic_history, R.color.amber_bg,
            R.string.feature_history_title, R.string.feature_history_desc),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Popula as feature rows dinamicamente
        features.forEach { feature ->
            val row = ItemFeatureRowBinding.inflate(layoutInflater, binding.featuresContainer, true)
            row.featureIcon.setImageResource(feature.iconRes)
            row.featureIcon.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), feature.iconBgColor)
            )
            row.featureTitle.setText(feature.titleRes)
            row.featureDesc.setText(feature.descRes)
        }

        binding.btnStart.setOnClickListener {
            findNavController().navigate(R.id.action_splash_to_scan)
        }

        binding.tvHowItWorks.setOnClickListener {
            findNavController().navigate(R.id.action_splash_to_howItWorks)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class FeatureItem(
        val iconRes: Int,
        val iconBgColor: Int,
        val titleRes: Int,
        val descRes: Int
    )
}