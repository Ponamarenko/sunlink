package com.sunportal.mobile

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val textView = findViewById<TextView>(R.id.textView)
        val updateButton = findViewById<Button>(R.id.updateButton)
        
        textView.text = "SunPortal v1.0.0\nБілд: 1"
        
        updateButton.setOnClickListener {
            Toast.makeText(this, "Checking updates...", Toast.LENGTH_SHORT).show()
        }
    }
}