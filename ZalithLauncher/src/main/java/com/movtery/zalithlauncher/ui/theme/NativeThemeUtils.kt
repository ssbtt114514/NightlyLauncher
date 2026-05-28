/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.theme

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.widget.Button
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.utilities.CorePalette
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.Variant
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.DarkMode

data class NativeColorScheme(
    @field:ColorInt val primary: Int,
    @field:ColorInt val onPrimary: Int,
    @field:ColorInt val primaryContainer: Int,
    @field:ColorInt val onPrimaryContainer: Int,
    @field:ColorInt val secondary: Int,
    @field:ColorInt val onSecondary: Int,
    @field:ColorInt val secondaryContainer: Int,
    @field:ColorInt val onSecondaryContainer: Int,
    @field:ColorInt val tertiary: Int,
    @field:ColorInt val onTertiary: Int,
    @field:ColorInt val tertiaryContainer: Int,
    @field:ColorInt val onTertiaryContainer: Int,
    @field:ColorInt val error: Int,
    @field:ColorInt val onError: Int,
    @field:ColorInt val errorContainer: Int,
    @field:ColorInt val onErrorContainer: Int,
    @field:ColorInt val background: Int,
    @field:ColorInt val onBackground: Int,
    @field:ColorInt val surface: Int,
    @field:ColorInt val onSurface: Int,
    @field:ColorInt val surfaceVariant: Int,
    @field:ColorInt val onSurfaceVariant: Int,
    @field:ColorInt val outline: Int,
    @field:ColorInt val outlineVariant: Int,
    @field:ColorInt val inverseSurface: Int,
    @field:ColorInt val inverseOnSurface: Int,
    @field:ColorInt val inversePrimary: Int,
    @field:ColorInt val surfaceDim: Int,
    @field:ColorInt val surfaceBright: Int,
    @field:ColorInt val surfaceContainerLowest: Int,
    @field:ColorInt val surfaceContainerLow: Int,
    @field:ColorInt val surfaceContainer: Int,
    @field:ColorInt val surfaceContainerHigh: Int,
    @field:ColorInt val surfaceContainerHighest: Int,
)

/**
 * 判断当前是否为暗色主题
 */
fun isDarkTheme(context: Context): Boolean {
    return when (AllSettings.launcherDarkMode.state) {
        DarkMode.Enable -> true
        DarkMode.Disable -> false
        DarkMode.FollowSystem -> {
            val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        }
    }
}

/**
 * 从当前主题设置生成原生主题配色调色板
 */
fun getColorScheme(context: Context): NativeColorScheme {
    val darkTheme = isDarkTheme(context)
    val seedColor = getSeedColor(context, darkTheme)
    return buildColorScheme(seedColor, darkTheme)
}

private fun buildColorScheme(seedColor: Int, darkTheme: Boolean): NativeColorScheme {
    val palettes = CorePalette.of(seedColor)

    val scheme = DynamicScheme(
        Hct.fromInt(seedColor),
        Variant.TONAL_SPOT,
        darkTheme,
        0.0,
        palettes.a1,
        palettes.a2,
        palettes.a3,
        palettes.n1,
        palettes.n2
    )

    val hctNeutral = Hct.fromInt(scheme.surface)
    val surfaceDim: Int
    val surfaceBright: Int
    val surfaceContainerLowest: Int
    val surfaceContainerLow: Int
    val surfaceContainer: Int
    val surfaceContainerHigh: Int
    val surfaceContainerHighest: Int

    if (darkTheme) {
        surfaceDim = Hct.from(hctNeutral.hue, hctNeutral.chroma, 6.0).toInt()
        surfaceBright = Hct.from(hctNeutral.hue, hctNeutral.chroma, 24.0).toInt()
        surfaceContainerLowest = Hct.from(hctNeutral.hue, hctNeutral.chroma, 4.0).toInt()
        surfaceContainerLow = Hct.from(hctNeutral.hue, hctNeutral.chroma, 10.0).toInt()
        surfaceContainer = Hct.from(hctNeutral.hue, hctNeutral.chroma, 12.0).toInt()
        surfaceContainerHigh = Hct.from(hctNeutral.hue, hctNeutral.chroma, 17.0).toInt()
        surfaceContainerHighest = Hct.from(hctNeutral.hue, hctNeutral.chroma, 22.0).toInt()
    } else {
        surfaceDim = Hct.from(hctNeutral.hue, hctNeutral.chroma, 87.0).toInt()
        surfaceBright = Hct.from(hctNeutral.hue, hctNeutral.chroma, 98.0).toInt()
        surfaceContainerLowest = Hct.from(hctNeutral.hue, hctNeutral.chroma, 100.0).toInt()
        surfaceContainerLow = Hct.from(hctNeutral.hue, hctNeutral.chroma, 96.0).toInt()
        surfaceContainer = Hct.from(hctNeutral.hue, hctNeutral.chroma, 94.0).toInt()
        surfaceContainerHigh = Hct.from(hctNeutral.hue, hctNeutral.chroma, 92.0).toInt()
        surfaceContainerHighest = Hct.from(hctNeutral.hue, hctNeutral.chroma, 90.0).toInt()
    }

    return NativeColorScheme(
        primary = scheme.primary,
        onPrimary = scheme.onPrimary,
        primaryContainer = scheme.primaryContainer,
        onPrimaryContainer = scheme.onPrimaryContainer,
        secondary = scheme.secondary,
        onSecondary = scheme.onSecondary,
        secondaryContainer = scheme.secondaryContainer,
        onSecondaryContainer = scheme.onSecondaryContainer,
        tertiary = scheme.tertiary,
        onTertiary = scheme.onTertiary,
        tertiaryContainer = scheme.tertiaryContainer,
        onTertiaryContainer = scheme.onTertiaryContainer,
        error = scheme.error,
        onError = scheme.onError,
        errorContainer = scheme.errorContainer,
        onErrorContainer = scheme.onErrorContainer,
        background = scheme.background,
        onBackground = scheme.onBackground,
        surface = scheme.surface,
        onSurface = scheme.onSurface,
        surfaceVariant = scheme.surfaceVariant,
        onSurfaceVariant = scheme.onSurfaceVariant,
        outline = scheme.outline,
        outlineVariant = scheme.outlineVariant,
        inverseSurface = scheme.inverseSurface,
        inverseOnSurface = scheme.inverseOnSurface,
        inversePrimary = scheme.inversePrimary,
        surfaceDim = surfaceDim,
        surfaceBright = surfaceBright,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
}

private fun getSeedColor(context: Context, darkTheme: Boolean): Int {
    return when (AllSettings.launcherColorTheme.state) {
        ColorThemeType.DYNAMIC -> {
            val ver = Build.VERSION.SDK_INT
            if (ver >= Build.VERSION_CODES.S) {
                if (ver >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (darkTheme) {
                        context.resources.getColor(android.R.color.system_primary_dark, context.theme)
                    } else {
                        context.resources.getColor(android.R.color.system_primary_light, context.theme)
                    }
                } else {
                    context.resources.getColor(android.R.color.system_accent1_200, context.theme)
                }
            } else {
                getPredefinedSeedColor(ColorThemeType.EMBERMIRE, darkTheme)
            }
        }
        ColorThemeType.CUSTOM -> AllSettings.launcherCustomColor.state
        else -> getPredefinedSeedColor(AllSettings.launcherColorTheme.state, darkTheme)
    }
}

private fun getPredefinedSeedColor(theme: ColorThemeType, darkTheme: Boolean): Int {
    return when (theme) {
        ColorThemeType.EMBERMIRE -> if (darkTheme) 0xFFFFB598.toInt() else 0xFFA63A17.toInt()
        ColorThemeType.VELVET_ROSE -> if (darkTheme) 0xFFF9B2D2.toInt() else 0xFF723D57.toInt()
        ColorThemeType.MISTWAVE -> if (darkTheme) 0xFFCEF3F9.toInt() else 0xFF426469.toInt()
        ColorThemeType.GLACIER -> if (darkTheme) 0xFF73D2FB.toInt() else 0xFF006684.toInt()
        ColorThemeType.VERDANTFIELD -> if (darkTheme) 0xFFD2C972.toInt() else 0xFF676014.toInt()
        ColorThemeType.URBAN_ASH -> if (darkTheme) 0xFFC7C6C6.toInt() else 0xFF5E5E5F.toInt()
        ColorThemeType.VERDANT_DAWN -> if (darkTheme) 0xFF8ED88E.toInt() else 0xFF004814.toInt()
        else -> 0xFFA63A17.toInt()
    }
}

private fun Button.applyThemeColors(
    colors: NativeColorScheme,
) {
    setTextColor(colors.primary)
    if (this is MaterialButton) {
        rippleColor = ColorStateList.valueOf(colors.primary)
    } else {
        (background as? RippleDrawable)?.setColor(ColorStateList.valueOf(colors.primary))
    }
}

fun MaterialAlertDialogBuilder.showThemed(): AlertDialog {
    val dialog = create()
    val colors = getColorScheme(dialog.context)

    dialog.setOnShowListener {
        dialog.window?.apply {
            background?.setTint(colors.surface)
        }
        dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(colors.onSurface)
        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(colors.onSurfaceVariant)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.applyThemeColors(colors)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.applyThemeColors(colors)
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.applyThemeColors(colors)
    }

    dialog.show()
    return dialog
}