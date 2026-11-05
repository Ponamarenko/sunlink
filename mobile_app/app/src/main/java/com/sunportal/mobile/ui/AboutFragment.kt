package com.sunportal.mobile.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sunportal.mobile.databinding.FragmentAboutBinding
import com.sunportal.mobile.R

/**
 * AboutFragment - экран "О приложении"
 */
class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
    }

    private fun setupUI() {
        binding.versionText.text = "Версия 1.2.0"
        binding.descriptionText.text = "SunLink - современное приложение для мониторинга солнечных систем"
        binding.copyrightText.text = "© 2026 Ponamarenko"
        
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}