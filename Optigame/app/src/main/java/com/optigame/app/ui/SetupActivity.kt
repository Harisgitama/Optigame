package com.optigame.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.optigame.app.databinding.ActivitySetupBinding
import com.optigame.app.utils.AdbManager

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var adbManager: AdbManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adbManager = AdbManager(this)

        binding.btnEnableWireless.setOnClickListener {
            openDeveloperOptions()
        }

        binding.btnConnect.setOnClickListener {
            val ip = binding.etIpAddress.text.toString().trim()
            val port = binding.etPort.text.toString().trim()
            if (ip.isEmpty() || port.isEmpty()) {
                Toast.makeText(this, "Masukkan IP dan Port terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            connectAdb(ip, port)
        }

        binding.btnSkip.setOnClickListener {
            goToMain()
        }

        binding.btnHowTo.setOnClickListener {
            showGuide()
        }
    }

    private fun openDeveloperOptions() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Buka: Pengaturan > Opsi Pengembang > Wireless Debugging", Toast.LENGTH_LONG).show()
        }
    }

    private fun connectAdb(ip: String, port: String) {
        binding.btnConnect.isEnabled = false
        binding.btnConnect.text = "Menghubungkan..."

        Thread {
            val result = adbManager.connect(ip, port)
            runOnUiThread {
                binding.btnConnect.isEnabled = true
                binding.btnConnect.text = "Hubungkan"
                if (result.success) {
                    Toast.makeText(this, "Terhubung ke $ip:$port", Toast.LENGTH_SHORT).show()
                    goToMain()
                } else {
                    Toast.makeText(this, "Gagal: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showGuide() {
        val guide = """
            Cara mengaktifkan Wireless Debugging:
            
            1. Buka Pengaturan > Tentang Ponsel
            2. Ketuk "Nomor Build" 7x untuk aktifkan Developer Mode
            3. Kembali ke Pengaturan > Opsi Pengembang
            4. Aktifkan "Wireless Debugging"
            5. Ketuk "Wireless Debugging" > "Pair device with pairing code"
            6. Catat IP:Port yang muncul
            7. Masukkan di kolom di bawah ini
        """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setTitle("Panduan Wireless Debugging")
            .setMessage(guide)
            .setPositiveButton("Mengerti") { d, _ -> d.dismiss() }
            .show()
    }
}
