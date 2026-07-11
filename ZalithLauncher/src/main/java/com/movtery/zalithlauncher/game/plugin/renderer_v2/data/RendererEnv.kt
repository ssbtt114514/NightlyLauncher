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

package com.movtery.zalithlauncher.game.plugin.renderer_v2.data

/**
 * 渲染器配置项状态、存储
 * @param envs 渲染器环境变量配置项
 */
class RendererEnv(
    val packageName: String,
    val envs: List<RendererConfig.Env>,
    genSummary: (metaString: String) -> String?,
) {


    /**
     * 获取该渲染器当前的环境变量配置
     */
    fun getEnv(): Map<String, String> {
        return emptyMap()
    }
}