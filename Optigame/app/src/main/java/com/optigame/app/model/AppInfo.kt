package com.optigame.app.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean = false
)
