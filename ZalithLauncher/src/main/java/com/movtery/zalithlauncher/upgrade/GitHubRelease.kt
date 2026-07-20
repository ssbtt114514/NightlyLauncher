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

package com.movtery.zalithlauncher.upgrade

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Releases API 返回的最新 release 数据结构
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("body")
    val body: String? = null,
    @SerialName("assets")
    val assets: List<Asset> = emptyList()
) {
    @Serializable
    data class Asset(
        @SerialName("name")
        val name: String,
        @SerialName("size")
        val size: Long,
        @SerialName("browser_download_url")
        val browserDownloadUrl: String
    )
}

/**
 * 将 GitHub Release 数据转换为应用内部使用的 [RemoteData]
 */
fun GitHubRelease.toRemoteData(): RemoteData {
    return RemoteData(
        code = parseVersionCode(tagName),
        version = tagName,
        createdAt = createdAt,
        files = assets
            .filter { it.name.endsWith(".apk", ignoreCase = true) }
            .map { asset ->
                RemoteData.RemoteFile(
                    fileName = asset.name,
                    uri = asset.browserDownloadUrl,
                    arch = detectArch(asset.name),
                    size = asset.size
                )
            },
        defaultBody = RemoteData.RemoteBody(
            language = "en",
            markdown = body ?: ""
        ),
        bodies = emptyList()
    )
}

/**
 * 从版本号字符串中提取版本代码，用于与 [BuildConfig.VERSION_CODE] 比较。
 * 支持 `v1.0`、`1.0.1` 等格式，按 major*10000 + minor*100 + patch 计算。
 */
private fun parseVersionCode(version: String): Int {
    val cleaned = version
        .trimStart { it == 'v' || it == 'V' }
        .substringBefore('-')
        .substringBefore('+')
    val parts = cleaned
        .split('.')
        .take(3)
        .mapNotNull { it.toIntOrNull() }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    return major * 10000 + minor * 100 + patch
}

/**
 * 根据文件名中的关键词判断安装包架构
 */
private fun detectArch(fileName: String): RemoteData.RemoteFile.Arch {
    val lower = fileName.lowercase()
    return when {
        lower.contains("arm64") || lower.contains("aarch64") -> RemoteData.RemoteFile.Arch.ARM64
        lower.contains("x86_64") || lower.contains("amd64") -> RemoteData.RemoteFile.Arch.X86_64
        lower.contains("x86") -> RemoteData.RemoteFile.Arch.X86
        lower.contains("arm") || lower.contains("armeabi") -> RemoteData.RemoteFile.Arch.ARM
        else -> RemoteData.RemoteFile.Arch.ALL
    }
}
