package com.sunportal.mobile.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.sunportal.mobile.R

class ConnectionFragment : Fragment() {

    private lateinit var serverAddress: EditText
    private lateinit var serverPort: EditText
    private lateinit var apiKey: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var testConnectionButton: Button
    private lateinit var saveButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connection, container, false)
        
        serverAddress = view.findViewById(R.id.server_address)
        serverPort = view.findViewById(R.id.server_port)
        apiKey = view.findViewById(R.id.api_key)
        username = view.findViewById(R.id.username)
        password = view.findViewById(R.id.password)
        testConnectionButton = view.findViewById(R.id.test_connection_button)
        saveButton = view.findViewById(R.id.save_button)
        
        // Заполняем сохраненные значения (если есть)
        loadConnectionSettings()
        
        testConnectionButton.setOnClickListener {
            testConnection()
        }
        
        saveButton.setOnClickListener {
            saveConnectionSettings()
        }
        
        return view
    }
    
    private fun loadConnectionSettings() {
        // TODO: Загрузить сохраненные настройки из SharedPreferences
        serverAddress.setText("192.168.1.100")
        serverPort.setText("8080")
        apiKey.setText("")
        username.setText("")
        password.setText("")
    }
    
    private fun testConnection() {
        val address = serverAddress.text.toString().trim()
        val port = serverPort.text.toString().trim()
        val key = apiKey.text.toString().trim()
        val user = username.text.toString().trim()
        val pass = password.text.toString().trim()
        
        if (address.isEmpty() || port.isEmpty()) {
            Toast.makeText(requireContext(), "Заповніть адресу та порт", Toast.LENGTH_SHORT).show()
            return
        }
        
        // TODO: Реализовать проверку подключения
        Toast.makeText(requireContext(), "Перевірка підключення...", Toast.LENGTH_SHORT).show()
        
        // Имитация проверки
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Toast.makeText(requireContext(), "Підключення успішне!", Toast.LENGTH_LONG).show()
        }, 2000)
    }
    
    private fun saveConnectionSettings() {
        val address = serverAddress.text.toString().trim()
        val port = serverPort.text.toString().trim()
        
        if (address.isEmpty() || port.isEmpty()) {
            Toast.makeText(requireContext(), "Заповніть адресу та порт", Toast.LENGTH_SHORT).show()
            return
        }
        
        // TODO: Сохранить настройки в SharedPreferences
        Toast.makeText(requireContext(), "Налаштування збережено!", Toast.LENGTH_SHORT).show()
    }
}