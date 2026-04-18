package com.optigame.app.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.optigame.app.model.AppInfo

object PackageUtils {

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return packages
            .map { appInfo ->
                AppInfo(
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName,
                    icon = try { pm.getApplicationIcon(appInfo.packageName) } catch (e: Exception) { null },
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedWith(compareBy({ it.isSystemApp }, { it.appName.lowercase() }))
    }

    fun getGameApps(context: Context): List<AppInfo> {
        return getInstalledApps(context).filter { !it.isSystemApp }
    }
}
