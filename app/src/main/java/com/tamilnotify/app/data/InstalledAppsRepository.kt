package com.tamilnotify.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean
)

class InstalledAppsRepository(private val context: Context) {

    suspend fun getInstalledUserApps(): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPackage = context.packageName

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val seenPackages = mutableSetOf<String>()
        val resultList = mutableListOf<AppInfoItem>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == selfPackage || seenPackages.contains(pkg)) {
                continue
            }
            seenPackages.add(pkg)

            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                resultList.add(
                    AppInfoItem(
                        packageName = pkg,
                        appName = label,
                        icon = icon,
                        isSystemApp = isSystem
                    )
                )
            } catch (_: Exception) {
                // Ignore unresolvable packages
            }
        }

        // Sort alphabetically by app name
        resultList.sortedBy { it.appName.lowercase() }
    }
}
