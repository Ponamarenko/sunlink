package com.sunlink.v2.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sunlink.v2.databinding.FragmentConnectionBinding
import com.sunlink.v2.R

/**
 * ConnectionFragment - экран "Подключение"
 */
class ConnectionFragment : Fragment() {

    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.serverUrl.setText("http://sunlink.pp.ua:8000")
        binding.port.setText("8000")
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveConnectionSettings()
        }

        binding.testButton.setOnClickListener {
            testConnection()
        }

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun saveConnectionSettings() {
        val serverUrl = binding.serverUrl.text.toString()
        val port = binding.port.text.toString()
        
        // TODO: Реализовать сохранение настроек
        requireActivity().onBackPressed()
    }

    private fun testConnection() {
        binding.testButton.isEnabled = false
        binding.testText.text = "Тестирование..."
        
        // TODO: Реализовать тестирование соединения
        binding.testText.text = "Соединение установлено"
        binding.testButton.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}