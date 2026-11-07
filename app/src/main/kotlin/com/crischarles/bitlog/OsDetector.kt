package com.crischarles.bitlog

import android.os.Build
import java.util.Locale

enum class AndroidOsFlavor(val profileKey: String, val displayName: String) {
    HYPER_OS("hyperos", "Xiaomi HyperOS"),
    ONE_UI("oneui", "Samsung One UI"),
    COLOR_OS("coloros", "ColorOS / OxygenOS"),
    ORIGIN_OS("originos", "Vivo OriginOS"),
    MAGIC_OS("magicos", "Honor MagicOS"),
    PIXEL_AOSP("pixel_aosp", "Google Pixel / AOSP"),
    GENERIC("generic", "Android Native");
}

object OsDetector {

    val currentFlavor: AndroidOsFlavor by lazy { detectFlavor() }

    private fun detectFlavor(): AndroidOsFlavor {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
        val brand = Build.BRAND.lowercase(Locale.getDefault())

        // 1. Xiaomi HyperOS / MIUI
        val miuiVersion = getSystemProperty("ro.miui.ui.version.name")
        val hyperOsVersion = getSystemProperty("ro.mi.os.version.name")
        if (hyperOsVersion.isNotEmpty() || miuiVersion.isNotEmpty() ||
            manufacturer in listOf("xiaomi", "redmi", "poco") ||
            brand in listOf("xiaomi", "redmi", "poco")
        ) {
            return AndroidOsFlavor.HYPER_OS
        }

        // 2. Samsung One UI
        val oneUiVersion = getSystemProperty("ro.build.version.oneui")
        val sepVersion = getSystemProperty("ro.build.version.sep")
        if (oneUiVersion.isNotEmpty() || sepVersion.isNotEmpty() || manufacturer == "samsung") {
            return AndroidOsFlavor.ONE_UI
        }

        // 3. Oppo / OnePlus / Realme (ColorOS / OxygenOS / Realme UI)
        val oppoRom = getSystemProperty("ro.build.version.opporom")
        val oplusRom = getSystemProperty("ro.build.version.oplusrom")
        val oxygenVersion = getSystemProperty("ro.oxygen.version")
        val realmeVersion = getSystemProperty("ro.build.version.realmeui")
        if (oppoRom.isNotEmpty() || oplusRom.isNotEmpty() || oxygenVersion.isNotEmpty() ||
            realmeVersion.isNotEmpty() || manufacturer in listOf("oppo", "oneplus", "realme")
        ) {
            return AndroidOsFlavor.COLOR_OS
        }

        // 4. Vivo / iQOO OriginOS / FuntouchOS
        val vivoOs = getSystemProperty("ro.vivo.os.version")
        val iqooOs = getSystemProperty("ro.iqoo.os.version")
        if (vivoOs.isNotEmpty() || iqooOs.isNotEmpty() || manufacturer in listOf("vivo", "iqoo")) {
            return AndroidOsFlavor.ORIGIN_OS
        }

        // 5. Huawei / Honor MagicOS / EMUI
        val magicOs = getSystemProperty("ro.build.version.magic")
        val emui = getSystemProperty("ro.build.version.emui")
        if (magicOs.isNotEmpty() || emui.isNotEmpty() || manufacturer in listOf("honor", "huawei")) {
            return AndroidOsFlavor.MAGIC_OS
        }

        // 6. Google Pixel
        if (manufacturer == "google" || brand == "google") {
            return AndroidOsFlavor.PIXEL_AOSP
        }

        return AndroidOsFlavor.GENERIC
    }

    fun getSystemProperty(key: String): String {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)
            (getMethod.invoke(null, key) as? String).orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
}

