package com.android.daviddev.ecoscancmem.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.daviddev.ecoscancmem.R
import com.android.daviddev.ecoscancmem.databinding.FragmentScanBinding
import com.android.daviddev.ecoscancmem.viewmodel.ScanViewModel

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ScanViewModel by viewModels()

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

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Se houver uma ação direta do scan para howItWorks, deve ser adicionada ao nav_graph
        // Por enquanto usamos a que já existe no Splash se compatível ou apenas voltamos.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
