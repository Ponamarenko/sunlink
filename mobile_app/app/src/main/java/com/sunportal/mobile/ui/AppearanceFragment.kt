package com.sunportal.mobile.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sunportal.mobile.databinding.FragmentAppearanceBinding
import com.sunportal.mobile.R

/**
 * AppearanceFragment - экран "Внешний вид"
 */
class AppearanceFragment : Fragment() {

    private var _binding: FragmentAppearanceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppearanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Установка текущих настроек
        binding.themeLight.isChecked = true
        binding.fontSizeMedium.isChecked = true
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.saveButton.setOnClickListener {
            saveAppearanceSettings()
        }
    }

    private fun saveAppearanceSettings() {
        val isDarkTheme = binding.themeDark.isChecked
        val fontSize = when {
            binding.fontSizeSmall.isChecked -> "small"
            binding.fontSizeLarge.isChecked -> "large"
            else -> "medium"
        }
        
        // TODO: Реализовать сохранение настроек
        requireActivity().onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}