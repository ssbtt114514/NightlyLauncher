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

package com.movtery.zalithlauncher.ui.screens.main.custom_home

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.movtery.zalithlauncher.path.PathManager

/**
 * 自定义主页事件
 *
 * 由主页 HTML/JS 通过 `ZalithLauncher.sendEvent(key, data)` 触发，
 * 最终被 [com.movtery.zalithlauncher.ui.activities.MainActivity.handleHomePageEvent] 处理。
 *
 * 已知事件契约：
 * - `url` 在浏览器打开链接，data 为 URL
 * - `check_update` 检查启动器更新
 * - `launch_game` 启动游戏，可选 data 形如 `server=...`
 * - `copy` 复制文本，data 为待复制内容
 * - `refresh_page` 刷新主页
 * - `share_game_log` 分享当前游戏日志
 */
data class HomePageEvent(
    val key: String,
    val data: String? = null
)

/**
 * 暴露给自定义主页 WebView 的 JavaScript 桥接对象。
 *
 * 在 HTML/JS 中通过 `ZalithLauncher.sendEvent(key, data)` 调用。
 */
class ZalithLauncherJsBridge(
    private val onEvent: (HomePageEvent) -> Unit,
    private val isDarkProvider: () -> Boolean,
    private val languageProvider: () -> String
) {
    @JavascriptInterface
    fun sendEvent(key: String, data: String?) {
        onEvent(HomePageEvent(key, data?.takeIf { it.isNotEmpty() }))
    }

    @JavascriptInterface
    fun isDarkTheme(): Boolean = isDarkProvider()

    @JavascriptInterface
    fun getLanguage(): String = languageProvider()
}

private const val TAG = "HomePageWebView"

/**
 * 自定义主页 WebView 容器。
 *
 * 复用启动器 `_PlayerSkin` 中的 WebViewAssetLoader 方案，加载 HTML+JS+CSS 主页。
 *
 * URL 资源映射：
 * - `https://appassets.androidplatform.net/assets/...` -> `assets/`
 * - `https://appassets.androidplatform.net/local/...`  -> `DIR_FILES_EXTERNAL/`
 * - `https://appassets.androidplatform.net/cache/...`  -> `DIR_CACHE_HOME_PAGE/`
 *
 * 主页 HTML 可通过相对路径引用同目录下的 JS / CSS / 图片资源。
 *
 * @param url WebViewAssetLoader 形式的 URL。
 * @param isDark 当前是否为深色主题，将注入到 `<html>` 上以供 CSS 切换主题。
 * @param language 当前语言标签（如 `zh`、`en`），将注入到 `<html lang="...">`。
 * @param onEvent 主页通过 JS 桥触发的事件回调。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HomePageWebView(
    url: String,
    isDark: Boolean,
    language: String,
    onEvent: (HomePageEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 使用 rememberUpdatedState 确保 JS 桥回调始终拿到最新的值，避免闭包过期
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentIsDark by rememberUpdatedState(isDark)
    val currentLanguage by rememberUpdatedState(language)

    val assetLoader = remember {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .addPathHandler(
                "/local/",
                WebViewAssetLoader.InternalStoragePathHandler(context, PathManager.DIR_FILES_EXTERNAL)
            )
            .addPathHandler(
                "/cache/",
                WebViewAssetLoader.InternalStoragePathHandler(context, PathManager.DIR_CACHE_HOME_PAGE)
            )
            .build()
    }

    val bridge = remember {
        ZalithLauncherJsBridge(
            onEvent = { currentOnEvent(it) },
            isDarkProvider = { currentIsDark },
            languageProvider = { currentLanguage }
        )
    }

    var webView by remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    mediaPlaybackRequiresUserGesture = false
                }
                setBackgroundColor(Color.Transparent.toArgb())
                overScrollMode = WebView.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = false
                // 注入 JS 桥
                addJavascriptInterface(bridge, "ZalithLauncher")
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // 主题与语言注入到 <html> 上，方便 CSS 切换
                        applyTheme(view, currentIsDark)
                        applyLanguage(view, currentLanguage)
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        Log.d(
                            TAG,
                            "${consoleMessage.message()} (line ${consoleMessage.lineNumber()})"
                        )
                        return true
                    }
                }

                loadUrl(url)
            }.also { webView = it }
        },
        update = { view ->
            // 主题切换时，运行时更新 <html> 上的 class，避免重新加载页面
            applyTheme(view, currentIsDark)
            applyLanguage(view, currentLanguage)
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
            webView = null
        }
    }
}

private fun applyTheme(view: WebView?, isDark: Boolean) {
    view?.evaluateJavascript(
        """
        (function() {
            var root = document.documentElement;
            if (root) {
                root.classList.toggle('dark', ${if (isDark) "true" else "false"});
                root.classList.toggle('light', ${if (!isDark) "true" else "false"});
            }
        })();
        """.trimIndent(),
        null
    )
}

private fun applyLanguage(view: WebView?, language: String) {
    view?.evaluateJavascript(
        """
        (function() {
            var root = document.documentElement;
            if (root) {
                root.setAttribute('lang', '$language');
            }
        })();
        """.trimIndent(),
        null
    )
}
