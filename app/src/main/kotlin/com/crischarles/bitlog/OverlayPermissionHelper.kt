package com.crischarles.bitlog

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object OverlayPermissionHelper {

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    /**
     * Launches the overlay permission settings screen, handling OEM-specific flavors
     * like Xiaomi HyperOS/MIUI background pop-up windows with graceful fallback.
     */
    fun launchOverlaySettings(context: Context) {
        val flavor = OsDetector.currentFlavor

        if (flavor == AndroidOsFlavor.HYPER_OS) {
            // Xiaomi HyperOS / MIUI special background pop-up permission
            val miuiIntent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity",
                )
                putExtra("extra_pkgname", context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (isIntentResolvable(context, miuiIntent)) {
                context.startActivity(miuiIntent)
                return
            }
        }

        // Standard Android overlay permission intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val standardIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (isIntentResolvable(context, standardIntent)) {
                context.startActivity(standardIntent)
                return
            }
        }

        // Generic fallback to application details
        val appDetails = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(appDetails)
    }

    private fun isIntentResolvable(context: Context, intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }
}

