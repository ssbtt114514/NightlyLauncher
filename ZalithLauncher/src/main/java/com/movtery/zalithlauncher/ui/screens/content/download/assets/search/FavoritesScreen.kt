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

package com.movtery.zalithlauncher.ui.screens.content.download.assets.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.favorites.FavoriteItem
import com.movtery.zalithlauncher.game.download.favorites.FavoriteType
import com.movtery.zalithlauncher.game.download.favorites.FavoritesManager
import com.movtery.zalithlauncher.ui.components.CheckChip
import com.movtery.zalithlauncher.ui.components.LittleTextLabel
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.AssetsIcon
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.formatNumberByLocale

/**
 * 收藏列表屏幕。
 *
 * 顶部是分类过滤器（参考游戏版本下载的 VersionTypeItem 实现，使用 CheckChip 多选）。
 * 列表中每一项是 [FavoriteItem]，点击后跳转到对应的下载/资源详情屏幕。
 *
 * @param mainScreenKey      主屏幕 Key（用于面包屑层级显示）
 * @param downloadScreenKey  下载屏幕 Key
 * @param downloadFavoritesScreenKey 收藏屏幕 Key
 * @param onSwapToDownload   点击资源卡片，跳转到资源下载详情
 * @param onSwapToGameVersion 点击游戏版本卡片，跳转到对应游戏版本安装页
 */
@Composable
fun FavoritesScreen(
    mainScreenKey: TitledNavKey?,
    downloadScreenKey: TitledNavKey?,
    downloadFavoritesScreenKey: TitledNavKey?,
    onSwapToDownload: (Platform, PlatformClasses, projectId: String, iconUrl: String?) -> Unit,
    onSwapToGameVersion: (versionId: String) -> Unit,
) {
    val favorites = FavoritesManager.state.value

    // 过滤器状态：选中的类型集合，空集表示显示全部
    var selectedTypes by rememberSaveable { mutableStateOf(setOf<FavoriteType>()) }

    val visibleItems = remember(favorites, selectedTypes) {
        if (selectedTypes.isEmpty()) favorites
        else favorites.filter { it.type in selectedTypes }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        //顶部过滤 Chip 栏
        FavoritesFilterBar(
            selectedTypes = selectedTypes,
            onToggle = { type ->
                selectedTypes = if (type in selectedTypes) {
                    selectedTypes - type
                } else {
                    selectedTypes + type
                }
            },
            onToggleAll = { selectedTypes = emptySet() }
        )

        if (visibleItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.favorites_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(visibleItems, key = { it.uniqueKey() }) { item ->
                    FavoriteCard(
                        modifier = Modifier.fillMaxWidth(),
                        item = item,
                        onClick = {
                            when (item.type) {
                                FavoriteType.GAME_VERSION -> {
                                    onSwapToGameVersion(item.projectId)
                                }
                                else -> {
                                    val platform = item.platform
                                        ?.let { runCatching { Platform.valueOf(it) }.getOrNull() }
                                        ?: Platform.MODRINTH
                                    val classes = typeToClasses(item.type) ?: PlatformClasses.MOD
                                    onSwapToDownload(platform, classes, item.projectId, item.iconUrl)
                                }
                            }
                        },
                        onRemove = { FavoritesManager.remove(item.type, item.projectId, item.platform) }
                    )
                }
            }
        }
    }
}

/**
 * 顶部过滤器栏，参考游戏的 VersionTypeItem 实现，使用 CheckChip 多选。
 */
@Composable
private fun FavoritesFilterBar(
    selectedTypes: Set<FavoriteType>,
    onToggle: (FavoriteType) -> Unit,
    onToggleAll: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //全部
        CheckChip(
            selected = selectedTypes.isEmpty(),
            onClick = { onToggleAll() },
            label = { Text(stringResource(R.string.favorites_filter_all)) }
        )
        //各个类型
        FavoriteType.entries.forEach { type ->
            CheckChip(
                selected = type in selectedTypes,
                onClick = { onToggle(type) },
                label = { Text(stringResource(type.displayNameRes)) }
            )
        }
    }
}

@Composable
private fun FavoriteCard(
    modifier: Modifier = Modifier,
    item: FavoriteItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val containerColor = cardColor()
    val contentColor = onCardColor()

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(all = 8.dp)
                .heightIn(min = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            //资源图标（GAME_VERSION 无）
            if (item.type != FavoriteType.GAME_VERSION && !item.iconUrl.isNullOrBlank()) {
                AssetsIcon(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .size(56.dp),
                    size = 56.dp,
                    iconUrl = item.iconUrl
                )
            } else if (item.type == FavoriteType.GAME_VERSION) {
                //游戏版本用草方块图标
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_old_grass_block),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                //标题行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    //游戏版本类型标签
                    item.gameVersionType?.let { vt ->
                        LittleTextLabel(text = vt)
                    }
                }
                //作者
                item.author?.takeIf { it.isNotBlank() }?.let { author ->
                    Text(
                        text = author,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
                //描述
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
                //底部标签：平台/类型/ModLoaders + 下载量
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    //类型标签
                    Text(
                        text = stringResource(item.type.displayNameRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.alpha(0.7f)
                    )
                    //平台
                    item.platform?.let { p ->
                        //通过 Platform 枚举转换存储的名称为友好显示格式
                        val displayName = remember(p) {
                            Platform.entries.find { it.name == p || it.displayName == p }?.displayName ?: p
                        }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.alpha(0.6f)
                        )
                    }
                    //ModLoaders
                    item.modLoaders.takeIf { it.isNotEmpty() }?.forEach { ml ->
                        Text(
                            text = ml,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.alpha(0.6f)
                        )
                    }
                    //下载量
                    if (item.downloadCount > 0) {
                        Text(
                            text = formatNumberByLocale(context, item.downloadCount),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.alpha(0.6f)
                        )
                    }
                }
            }

            //右侧取消收藏按钮
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.ic_favorite_filled),
                    contentDescription = stringResource(R.string.favorites_remove),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * [FavoriteType] 转 [PlatformClasses]，仅资源类有效。
 * 游戏版本返回 null。
 */
private fun typeToClasses(type: FavoriteType): PlatformClasses? = when (type) {
    FavoriteType.MOD -> PlatformClasses.MOD
    FavoriteType.MOD_PACK -> PlatformClasses.MOD_PACK
    FavoriteType.RESOURCE_PACK -> PlatformClasses.RESOURCE_PACK
    FavoriteType.SAVES -> PlatformClasses.SAVES
    FavoriteType.SHADERS -> PlatformClasses.SHADERS
    FavoriteType.GAME_VERSION -> null
}
