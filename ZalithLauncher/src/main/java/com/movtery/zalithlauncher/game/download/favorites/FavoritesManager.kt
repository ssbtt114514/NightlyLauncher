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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV

/**
 * 收藏管理器。
 *
 * 使用 MMKV 持久化 JSON 字符串列表，多进程安全。
 * 通过 [state] 暴露当前收藏列表，UI 可观察变化。
 */
object FavoritesManager {
    private const val KEY_FAVORITES = "favorites_json"

    private val mmkv: MMKV by lazy {
        MMKV.mmkvWithID("Favorites", MMKV.MULTI_PROCESS_MODE)
    }
    private val gson by lazy { Gson() }
    private val type by lazy { object : TypeToken<List<FavoriteItem>>() {}.type }

    private val _state = mutableStateOf(loadAll())
    /** 当前收藏列表，UI 观察用 */
    val state: State<List<FavoriteItem>> = _state

    /** 当前收藏数量 */
    fun size(): Int = _state.value.size

    /**
     * 判断某个资源是否已收藏。
     *
     * 注意：只通过 [projectId] + [platform] 匹配，不依赖 [type]，
     * 这样即使历史数据中类型识别错误，也能正确识别为已收藏。
     *
     * @param type       资源类型（保留参数以保持调用兼容，但不参与匹配）
     * @param projectId  资源 ID（游戏版本为版本 ID）
     * @param platform   平台名称（游戏版本为 null）
     */
    fun isFavorite(type: FavoriteType, projectId: String, platform: String?): Boolean {
        return _state.value.any { it.projectId == projectId && it.platform == platform }
    }

    /** 判断某个 [FavoriteItem] 是否已收藏 */
    fun isFavorite(item: FavoriteItem): Boolean = isFavorite(item.type, item.projectId, item.platform)

    /**
     * 切换收藏状态：未收藏则添加，已收藏则移除。
     *
     * 如果已存在相同 [projectId] + [platform] 的收藏：
     * - 当 [item.type] 与已记录的类型相同，则取消收藏（移除）
     * - 当 [item.type] 与已记录的类型不同，则更新为新的 [item]（修正历史数据类型错误）
     * @return 切换后是否处于收藏状态
     */
    fun toggle(item: FavoriteItem): Boolean {
        val current = _state.value.toMutableList()
        val existIndex = current.indexOfFirst { it.uniqueKey() == item.uniqueKey() }
        return if (existIndex >= 0) {
            val existing = current[existIndex]
            if (existing.type == item.type) {
                //类型相同，取消收藏
                current.removeAt(existIndex)
                persist(current)
                false
            } else {
                //类型不同，更新为新的类型（修正历史数据）
                current[existIndex] = item
                persist(current)
                true
            }
        } else {
            current.add(0, item)
            persist(current)
            true
        }
    }

    /**
     * 添加收藏（不重复添加）。
     */
    fun add(item: FavoriteItem) {
        if (isFavorite(item)) return
        val current = _state.value.toMutableList()
        current.add(0, item)
        persist(current)
    }

    /**
     * 移除收藏，返回是否成功移除。
     *
     * 注意：只通过 [projectId] + [platform] 匹配，不依赖 [type]。
     */
    fun remove(type: FavoriteType, projectId: String, platform: String?): Boolean {
        val current = _state.value.toMutableList()
        val removed = current.removeAll { it.projectId == projectId && it.platform == platform }
        if (removed) persist(current)
        return removed
    }

    /** 重新从 MMKV 加载并刷新 Compose 状态 */
    fun reload() {
        _state.value = loadAll()
    }

    private fun loadAll(): List<FavoriteItem> {
        val json = mmkv.decodeString(KEY_FAVORITES, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<FavoriteItem>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<FavoriteItem>) {
        val json = gson.toJson(list)
        mmkv.encode(KEY_FAVORITES, json)
        _state.value = list
    }
}
