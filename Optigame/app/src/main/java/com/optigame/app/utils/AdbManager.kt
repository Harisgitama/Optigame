package com.optigame.app.utils

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

data class AdbResult(val success: Boolean, val message: String)

class AdbManager(private val context: Context) {

    private var connectedIp: String? = null
    private var connectedPort: String? = null

    fun connect(ip: String, port: String): AdbResult {
        return try {
            // Test basic TCP connection to ADB port
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(ip, port.toInt()), 3000)
            socket.close()
            connectedIp = ip
            connectedPort = port
            AdbResult(true, "Connected to $ip:$port")
        } catch (e: Exception) {
            AdbResult(false, "Tidak bisa terhubung: ${e.message}")
        }
    }

    fun executeShell(command: String): AdbResult {
        return try {
            // Try using adb binary if available (rooted or with adb installed)
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "adb shell $command"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = reader.readLines().joinToString("\n")
            val error = errorReader.readLines().joinToString("\n")
            process.waitFor()

            if (process.exitValue() == 0) {
                AdbResult(true, output)
            } else {
                AdbResult(false, error.ifEmpty { "Command failed" })
            }
        } catch (e: Exception) {
            AdbResult(false, "ADB tidak tersedia: ${e.message}")
        }
    }

    fun isConnected(): Boolean = connectedIp != null

    fun getConnectionInfo(): String {
        return if (isConnected()) "$connectedIp:$connectedPort" else "Tidak terhubung"
    }

    fun buildDownscaleCommand(packageName: String, factor: Float): String {
        val f = "%.1f".format(factor)
        return "adb shell device_config put game_overlay $packageName " +
                "\"mode=2,downscaleFactor=${f}:mode=3,downscaleFactor=${f}\""
    }

    fun buildResetCommand(packageName: String): String {
        return "adb shell device_config delete game_overlay $packageName"
    }
}
