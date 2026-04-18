package com.optigame.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.optigame.app.R
import com.optigame.app.databinding.ActivityMainBinding
import com.optigame.app.model.AppInfo
import com.optigame.app.ui.adapter.AppAdapter
import com.optigame.app.utils.AdbManager
import com.optigame.app.utils.PackageUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appAdapter: AppAdapter
    private lateinit var adbManager: AdbManager
    private var allApps = mutableListOf<AppInfo>()
    private var selectedApp: AppInfo? = null
    private var downscaleFactor = 0.9f
    private var isSidebarOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adbManager = AdbManager(this)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupSidebar()
        loadApps()
    }

    private fun setupToolbar() {
        binding.btnMenu.setOnClickListener {
            toggleSidebar()
        }
        binding.btnRefresh.setOnClickListener {
            loadApps()
        }
    }

    private fun setupRecyclerView() {
        appAdapter = AppAdapter(
            onItemClick = { app -> launchApp(app) },
            onItemLongClick = { app -> showAppOptions(app) }
        )
        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSidebar() {
        binding.overlay.setOnClickListener { closeSidebar() }

        // Scale Seekbar
        binding.seekbarScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minScale = 0.3f
                val maxScale = 1.0f
                downscaleFactor = minScale + (progress / 100f) * (maxScale - minScale)
                val percent = (downscaleFactor * 100).toInt()
                binding.tvScaleValue.text = "$percent%"
                updateScalePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.seekbarScale.progress = 60 // default 0.9

        // Preset buttons
        binding.btnPreset90.setOnClickListener { setScale(0.9f) }
        binding.btnPreset75.setOnClickListener { setScale(0.75f) }
        binding.btnPreset50.setOnClickListener { setScale(0.5f) }

        // Apply button
        binding.btnApplyDownscale.setOnClickListener { applyDownscale() }

        // Reset button
        binding.btnResetDownscale.setOnClickListener { resetDownscale() }

        updateScalePreview()
    }

    private fun setScale(scale: Float) {
        downscaleFactor = scale
        val minScale = 0.3f
        val maxScale = 1.0f
        val progress = ((scale - minScale) / (maxScale - minScale) * 100).toInt()
        binding.seekbarScale.progress = progress
        val percent = (scale * 100).toInt()
        binding.tvScaleValue.text = "$percent%"
        updateScalePreview()
    }

    private fun updateScalePreview() {
        val percent = (downscaleFactor * 100).toInt()
        val app = selectedApp
        if (app != null) {
            binding.tvSelectedGameSidebar.text = app.appName
            binding.tvCommandPreview.text = "adb shell device_config put game_overlay\n${app.packageName}\nmode=2,downscaleFactor=${"%.1f".format(downscaleFactor)}:mode=3,downscaleFactor=${"%.1f".format(downscaleFactor)}"
        } else {
            binding.tvSelectedGameSidebar.text = "Belum ada game dipilih"
            binding.tvCommandPreview.text = "Pilih game dari daftar terlebih dahulu"
        }
        binding.tvQualityDesc.text = when {
            downscaleFactor >= 0.9f -> "Kualitas Tinggi - Penurunan minimal"
            downscaleFactor >= 0.7f -> "Kualitas Sedang - Keseimbangan performa"
            downscaleFactor >= 0.5f -> "Kualitas Rendah - Performa optimal"
            else -> "Kualitas Sangat Rendah - FPS maksimal"
        }
    }

    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvApps.visibility = View.GONE

        Thread {
            val apps = PackageUtils.getInstalledApps(this)
            allApps = apps.toMutableList()
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                binding.rvApps.visibility = View.VISIBLE
                appAdapter.submitList(apps)
                binding.tvAppCount.text = "${apps.size} aplikasi"
            }
        }.start()
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        appAdapter.submitList(filtered)
        binding.tvAppCount.text = "${filtered.size} aplikasi"
    }

    private fun launchApp(app: AppInfo) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Tidak bisa membuka ${app.appName}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppOptions(app: AppInfo) {
        selectedApp = app
        updateScalePreview()

        val options = arrayOf(
            "ℹ️  Info Aplikasi",
            "▶️  Jalankan Aplikasi",
            "📋  Copy Package Name",
            "🎮  Pilih & Downscale"
        )

        android.app.AlertDialog.Builder(this, R.style.Theme_Optigame_Dialog)
            .setTitle(app.appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openAppInfo(app)
                    1 -> launchApp(app)
                    2 -> copyActivity(app)
                    3 -> {
                        selectedApp = app
                        updateScalePreview()
                        openSidebar()
                        Toast.makeText(this, "${app.appName} dipilih", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun openAppInfo(app: AppInfo) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:${app.packageName}")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak bisa membuka info aplikasi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyActivity(app: AppInfo) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("package", app.packageName)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Disalin: ${app.packageName}", Toast.LENGTH_SHORT).show()
    }

    private fun applyDownscale() {
        val app = selectedApp
        if (app == null) {
            Toast.makeText(this, "Pilih game terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnApplyDownscale.isEnabled = false
        binding.btnApplyDownscale.text = "Menerapkan..."

        Thread {
            val cmd = "device_config put game_overlay ${app.packageName} " +
                "\"mode=2,downscaleFactor=${downscaleFactor}:mode=3,downscaleFactor=${downscaleFactor}\""
            val result = adbManager.executeShell(cmd)

            runOnUiThread {
                binding.btnApplyDownscale.isEnabled = true
                binding.btnApplyDownscale.text = "Terapkan"
                if (result.success) {
                    Toast.makeText(this,
                        "✅ Downscale ${(downscaleFactor*100).toInt()}% diterapkan ke ${app.appName}",
                        Toast.LENGTH_LONG).show()
                } else {
                    // Show command to copy manually
                    showManualCommand(app, cmd)
                }
            }
        }.start()
    }

    private fun resetDownscale() {
        val app = selectedApp
        if (app == null) {
            Toast.makeText(this, "Pilih game terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            val cmd = "device_config delete game_overlay ${app.packageName}"
            val result = adbManager.executeShell(cmd)
            runOnUiThread {
                if (result.success) {
                    Toast.makeText(this, "✅ Reset downscale ${app.appName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Gagal reset. Coba manual via ADB.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showManualCommand(app: AppInfo, cmd: String) {
        val fullCmd = "adb shell $cmd"
        android.app.AlertDialog.Builder(this, R.style.Theme_Optigame_Dialog)
            .setTitle("Jalankan Manual di PC")
            .setMessage("ADB tidak terhubung. Salin dan jalankan perintah ini di PC:\n\n$fullCmd")
            .setPositiveButton("Salin Perintah") { d, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("adb", fullCmd))
                Toast.makeText(this, "Perintah disalin!", Toast.LENGTH_SHORT).show()
                d.dismiss()
            }
            .setNegativeButton("Tutup") { d, _ -> d.dismiss() }
            .show()
    }

    private fun toggleSidebar() {
        if (isSidebarOpen) closeSidebar() else openSidebar()
    }

    private fun openSidebar() {
        isSidebarOpen = true
        binding.sidebarLayout.visibility = View.VISIBLE
        binding.overlay.visibility = View.VISIBLE
        binding.sidebarLayout.animate().translationX(0f).setDuration(300).start()
        binding.overlay.animate().alpha(1f).setDuration(300).start()
    }

    private fun closeSidebar() {
        isSidebarOpen = false
        val width = binding.sidebarLayout.width.toFloat()
        binding.sidebarLayout.animate().translationX(-width).setDuration(300).withEndAction {
            binding.sidebarLayout.visibility = View.GONE
        }.start()
        binding.overlay.animate().alpha(0f).setDuration(300).withEndAction {
            binding.overlay.visibility = View.GONE
        }.start()
    }
}
