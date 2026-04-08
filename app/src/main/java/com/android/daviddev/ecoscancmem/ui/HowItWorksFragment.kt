package com.android.daviddev.ecoscancmem.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.daviddev.ecoscancmem.R
import com.android.daviddev.ecoscancmem.databinding.FragmentHowItWorksBinding
import com.android.daviddev.ecoscancmem.databinding.ItemHowStepBinding
import com.android.daviddev.ecoscancmem.databinding.ItemRecycleCodeBinding

class HowItWorksFragment : Fragment() {
    private var _binding: FragmentHowItWorksBinding? = null
    private val binding get() = _binding!!

    // Dados dos passos
    private data class Step(val number: String, val title: String, val desc: String)

    private val steps = listOf(
        Step("1",
            "Aponte a câmara",
            "Aponte para um objeto ou rótulo de embalagem. A app analisa automaticamente em tempo real."
        ),
        Step("2",
            "Aguarde a análise",
            "O ML Kit identifica o objeto e lê o código de reciclagem no rótulo via OCR. Mantenha o dispositivo estável."
        ),
        Step("3",
            "Veja o resultado",
            "Descubra o material, o ecoponto correto, o impacto ambiental e dicas de preparação."
        ),
        Step("4",
            "Guarde no histórico",
            "Registe os scans e acompanhe o CO2 total poupado ao longo do tempo."
        )
    )

    // Dados dos códigos de reciclagem
    private data class RecycleCode(
        val number: String,
        val sigla: String,
        val name: String,
        val examples: String,
        val destination: String,
        @ColorRes val badgeBg: Int,
        @ColorRes val badgeText: Int,
        @ColorRes val destColor: Int
    )

    private val recycleCodes = listOf(
        RecycleCode("1",  "PET",   "PET - Politereftalato de etileno",
            "Garrafas de água, refrigerantes, sumos",
            "Ecoponto Amarelo",
            R.color.code_bg_blue, R.color.code_text_blue, R.color.green_primary),

        RecycleCode("2",  "HDPE",  "HDPE - Polietileno de alta densidade",
            "Frascos de detergente, garrafões, tampas",
            "Ecoponto Amarelo",
            R.color.code_bg_green, R.color.code_text_green, R.color.green_primary),

        RecycleCode("3",  "PVC",   "PVC - Policloreto de vinilo",
            "Tubagens, embalagens alimentares, brinquedos",
            "Lixo geral - não reciclar",
            R.color.code_bg_yellow, R.color.code_text_yellow, R.color.color_danger),

        RecycleCode("4",  "LDPE",  "LDPE - Polietileno de baixa densidade",
            "Sacos de plástico, filme alimentar, sacos de congelados",
            "Ecoponto Amarelo",
            R.color.code_bg_green, R.color.code_text_green, R.color.green_primary),

        RecycleCode("5",  "PP",    "PP - Polipropileno",
            "Tampas, copos, embalagens de iogurte, palhinhas",
            "Ecoponto Amarelo",
            R.color.code_bg_green, R.color.code_text_green, R.color.green_primary),

        RecycleCode("6",  "PS",    "PS - Poliestireno",
            "Esferovite, copos de café, tabuleiros de carne",
            "Lixo geral",
            R.color.code_bg_red, R.color.code_text_red, R.color.color_danger),

        RecycleCode("7",  "OTHER", "OTHER - Outros plásticos mistos",
            "Embalagens mistas, acrílico, policarbonato",
            "Verificar localmente",
            R.color.code_bg_gray, R.color.code_text_gray, R.color.color_danger),

        RecycleCode("20", "PAP",   "PAP 20 - Cartão canelado",
            "Caixas de cartão, embalagens de transporte",
            "Ecoponto Azul",
            R.color.code_bg_teal, R.color.code_text_teal, R.color.color_info),

        RecycleCode("21", "PAP",   "PAP 21 - Cartão liso",
            "Caixas de cereais, embalagens de medicamentos",
            "Ecoponto Azul",
            R.color.code_bg_teal, R.color.code_text_teal, R.color.color_info),

        RecycleCode("22", "PAP",   "PAP 22 - Papel",
            "Jornais, revistas, papel de escritório",
            "Ecoponto Azul",
            R.color.code_bg_teal, R.color.code_text_teal, R.color.color_info),

        RecycleCode("41", "ALU",   "ALU 41 - Alumínio",
            "Latas de bebida, papel de alumínio, cápsulas",
            "Ecoponto Amarelo",
            R.color.code_bg_green, R.color.code_text_green, R.color.green_primary),

        RecycleCode("70", "GL",    "GL 70 - Vidro incolor",
            "Garrafas transparentes, frascos de conserva",
            "Ecoponto Verde",
            R.color.code_bg_green, R.color.code_text_green, R.color.green_700),

        RecycleCode("71", "GL",    "GL 71 - Vidro verde",
            "Garrafas de vinho verde, espumante",
            "Ecoponto Verde",
            R.color.code_bg_green, R.color.code_text_green, R.color.green_700),

        RecycleCode("72", "GL",    "GL 72 - Vidro castanho",
            "Garrafas de cerveja, vinho tinto",
            "Ecoponto Verde",
            R.color.code_bg_green, R.color.code_text_green, R.color.green_700)
    )

    // Lifecycle
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHowItWorksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        populateSteps()
        populateCodes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Preenchimento dinâmico
    private fun populateSteps() {
        val stepViews = listOf(binding.step1, binding.step2, binding.step3, binding.step4)
        steps.forEachIndexed { i, step ->
            val row = ItemHowStepBinding.bind(stepViews[i].root)
            row.stepNumber.text = step.number
            row.stepTitle.text  = step.title
            row.stepDesc.text   = step.desc
        }
    }

    private fun populateCodes() {
        recycleCodes.forEach { code ->
            val row = ItemRecycleCodeBinding.inflate(
                layoutInflater, binding.codesContainer, true
            )
            row.tvCodeNumber.text   = code.number
            row.tvCodeSigla.text    = code.sigla
            row.tvCodeName.text     = code.name
            row.tvCodeExamples.text = code.examples
            row.tvCodeDest.text     = code.destination

            val badgeBg   = ContextCompat.getColor(requireContext(), code.badgeBg)
            val badgeText = ContextCompat.getColor(requireContext(), code.badgeText)
            val destColor = ContextCompat.getColor(requireContext(), code.destColor)

            row.codeBadge.setBackgroundColor(badgeBg)
            row.tvCodeNumber.setTextColor(badgeText)
            row.tvCodeSigla.setTextColor(badgeText)
            row.tvCodeDest.setTextColor(destColor)
        }
    }
}