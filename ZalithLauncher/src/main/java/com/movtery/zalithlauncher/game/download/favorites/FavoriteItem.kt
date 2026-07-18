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

package com.movtery.zalithlauncher.game.download.favorites

import com.google.gson.annotations.SerializedName

/**
 * 收藏项的统一数据模型。
 *
 * 用一个数据类承载所有类型的收藏，以便于持久化与 UI 复用。
 * 不同类型的字段会有部分为空，例如游戏版本不需要 [platform] / [modLoaders] / [iconUrl]。
 *
 * @param type         收藏类型
 * @param platform     来源平台（仅资源类有效，游戏版本为 null）
 * @param projectId    资源 ID 或游戏版本 ID（同类型 + 同 ID 视为同一收藏）
 * @param title        展示标题
 * @param iconUrl      资源图标 URL（游戏版本无）
 * @param description  简短描述
 * @param author       作者（游戏版本无）
 * @param modLoaders   模组加载器列表（如 ["Fabric","Forge"]，仅模组/整合包/资源包/光影包/存档可能非空）
 * @param categories   分类标签文本列表
 * @param downloadCount 下载量
 * @param followerCount 收藏量
 * @param gameVersionType 游戏版本类型（release / snapshot / april_fools / old），仅 [FavoriteType.GAME_VERSION] 有效
 * @param gameReleaseTime 游戏版本发布时间，仅 [FavoriteType.GAME_VERSION] 有效
 * @param addTime      收藏时间戳（毫秒）
 */
data class FavoriteItem(
    @SerializedName("type") val type: FavoriteType,
    @SerializedName("platform") val platform: String? = null,
    @SerializedName("projectId") val projectId: String,
    @SerializedName("title") val title: String,
    @SerializedName("iconUrl") val iconUrl: String? = null,
    @SerializedName("description") val description: String = "",
    @SerializedName("author") val author: String? = null,
    @SerializedName("modLoaders") val modLoaders: List<String> = emptyList(),
    @SerializedName("categories") val categories: List<String> = emptyList(),
    @SerializedName("downloadCount") val downloadCount: Long = 0L,
    @SerializedName("followerCount") val followerCount: Long = 0L,
    @SerializedName("gameVersionType") val gameVersionType: String? = null,
    @SerializedName("gameReleaseTime") val gameReleaseTime: String? = null,
    @SerializedName("addTime") val addTime: Long = System.currentTimeMillis()
) {
    /**
     * 唯一标识：平台 + 项目 ID。
     *
     * 注意：不包含 [type]，这样当资源类型识别错误并重新收藏时，
     * 新的收藏会覆盖旧记录，避免出现重复条目。
     */
    fun uniqueKey(): String = "${platform ?: ""}|$projectId"
}
