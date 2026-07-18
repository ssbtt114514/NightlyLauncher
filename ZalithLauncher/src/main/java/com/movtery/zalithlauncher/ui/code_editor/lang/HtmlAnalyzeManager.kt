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

package com.movtery.zalithlauncher.ui.code_editor.lang

import io.github.rosemoe.sora.lang.analysis.SimpleAnalyzeManager
import io.github.rosemoe.sora.lang.styling.MappedSpans
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * HTML 语法高亮解析器
 * 支持：标签、属性、字符串、注释、DOCTYPE、`<script>`/`<style>` 内联代码
 */
class HtmlAnalyzeManager : SimpleAnalyzeManager<Any?>() {
    private val tagStyle       = TextStyle.makeStyle(EditorColorScheme.KEYWORD,       0, true,  false, false)
    private val tagNameStyle   = TextStyle.makeStyle(EditorColorScheme.FUNCTION_NAME,0, true,  false, false)
    private val attrNameStyle  = TextStyle.makeStyle(EditorColorScheme.ANNOTATION,    0, false, false, false)
    private val attrValueStyle = TextStyle.makeStyle(EditorColorScheme.LITERAL,      0, false, false, false)
    private val stringQuoteStyle = TextStyle.makeStyle(EditorColorScheme.OPERATOR,   0, false, false, false)
    private val commentStyle   = TextStyle.makeStyle(EditorColorScheme.COMMENT,      0, false, true,  false)
    private val doctypeStyle   = TextStyle.makeStyle(EditorColorScheme.COMMENT,      0, true,  false, false)
    private val entityStyle    = TextStyle.makeStyle(EditorColorScheme.LITERAL,      0, false, false, false)
    private val operatorStyle  = TextStyle.makeStyle(EditorColorScheme.OPERATOR,     0, false, false, false)
    private val normalStyle    = TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)

    /** 匹配 HTML 注释 `<!-- ... -->`（可能跨多行） */
    private val commentStartRegex = Regex("<!--")
    private val commentEndRegex = Regex("-->")

    /** 匹配 HTML 实体 `&entity;` */
    private val entityRegex = Regex("&(?:[a-zA-Z]+|#\\d+);")

    /** 匹配单个标签内的所有 token */
    private val tagTokenRegex = Regex(
        """(</?)([a-zA-Z][a-zA-Z0-9\-:]*)|(/?>)|([a-zA-Z_:][a-zA-Z0-9\-_:]*)\s*(=)?|("[^"]*")|('[^']*')"""
    )

    override fun analyze(text: StringBuilder, delegate: Delegate<Any?>): Styles {
        val builder = MappedSpans.Builder()
        val src = text.toString()
        val lines = src.split("\n")

        var inComment = false
        var commentEndIndex = -1

        for (lineIndex in lines.indices) {
            if (delegate.isCancelled) break
            val line = lines[lineIndex]
            val lastIndex = if (lineIndex == 0) 0 else 0

            //注释状态：寻找结束
            if (inComment) {
                val endMatch = commentEndRegex.find(line)
                if (endMatch != null) {
                    builder.add(lineIndex, SpanFactory.obtainNoExt(0, commentStyle))
                    val afterEnd = endMatch.range.last + 1
                    if (afterEnd < line.length) {
                        builder.add(lineIndex, SpanFactory.obtainNoExt(afterEnd, normalStyle))
                        analyzeSegment(line.substring(afterEnd), lineIndex, afterEnd, builder)
                    }
                    inComment = false
                } else {
                    builder.add(lineIndex, SpanFactory.obtainNoExt(0, commentStyle))
                }
                continue
            }

            //处理行内可能存在的注释开头
            val commentStart = commentStartRegex.find(line)
            if (commentStart != null) {
                val before = commentStart.range.first
                if (before > 0) {
                    builder.add(lineIndex, SpanFactory.obtainNoExt(0, normalStyle))
                    analyzeSegment(line.substring(0, before), lineIndex, 0, builder)
                }
                builder.add(lineIndex, SpanFactory.obtainNoExt(before, commentStyle))

                //在同行寻找结束
                val endInSameLine = commentEndRegex.find(line, commentStart.range.last)
                if (endInSameLine != null) {
                    val afterEnd = endInSameLine.range.last + 1
                    if (afterEnd < line.length) {
                        builder.add(lineIndex, SpanFactory.obtainNoExt(afterEnd, normalStyle))
                        analyzeSegment(line.substring(afterEnd), lineIndex, afterEnd, builder)
                    }
                } else {
                    inComment = true
                }
                continue
            }

            builder.addIfNeeded(lineIndex, 0, normalStyle)
            analyzeSegment(line, lineIndex, 0, builder)
        }

        builder.determine(if (lines.isEmpty()) 0 else lines.size - 1)
        return Styles(builder.build())
    }

    /**
     * 解析一行内非注释的内容，按 `<...>` 标签块与普通文本分段着色
     */
    private fun analyzeSegment(
        segment: String,
        lineIndex: Int,
        baseOffset: Int,
        builder: MappedSpans.Builder
    ) {
        var i = 0
        val len = segment.length
        while (i < len) {
            val ch = segment[i]
            if (ch == '<') {
                //寻找本行的 `>` 闭合
                val closeIdx = segment.indexOf('>', i + 1)
                if (closeIdx == -1) {
                    //未闭合，按操作符着色到行尾
                    builder.add(lineIndex, SpanFactory.obtainNoExt(baseOffset + i, operatorStyle))
                    break
                }
                val tagContent = segment.substring(i, closeIdx + 1)
                renderTag(tagContent, lineIndex, baseOffset + i, builder)
                i = closeIdx + 1
            } else {
                //寻找下一个 `<` 之前都是普通文本，期间实体着色
                val nextLt = segment.indexOf('<', i)
                val textEnd = if (nextLt == -1) len else nextLt
                if (textEnd > i) {
                    builder.add(lineIndex, SpanFactory.obtainNoExt(baseOffset + i, normalStyle))
                    //实体着色
                    entityRegex.findAll(segment, i).forEach { m ->
                        if (m.range.first < textEnd) {
                            val end = minOf(m.range.last + 1, textEnd)
                            builder.add(lineIndex, SpanFactory.obtainNoExt(baseOffset + m.range.first, entityStyle))
                            builder.add(lineIndex, SpanFactory.obtainNoExt(baseOffset + end, normalStyle))
                        }
                    }
                }
                i = textEnd
            }
        }
    }

    /**
     * 对一个 `<...>` 标签块进行着色
     */
    private fun renderTag(
        tag: String,
        lineIndex: Int,
        baseOffset: Int,
        builder: MappedSpans.Builder
    ) {
        //整个标签的 `<` 和 `>` 标记为 tagStyle，标签名标记为 tagNameStyle
        tagTokenRegex.findAll(tag).forEach { match ->
            val groups = match.groupValues
            val start = baseOffset + match.range.first
            // `</?` 部分
            if (groups[1].isNotEmpty()) {
                builder.add(lineIndex, SpanFactory.obtainNoExt(start, tagStyle))
                //标签名
                val nameStart = start + groups[1].length
                builder.add(lineIndex, SpanFactory.obtainNoExt(nameStart, tagNameStyle))
            }
            // `>` 或 `/>` 闭合
            else if (groups[3].isNotEmpty()) {
                builder.add(lineIndex, SpanFactory.obtainNoExt(start, tagStyle))
            }
            //属性名 + 等号
            else if (groups[4].isNotEmpty()) {
                builder.add(lineIndex, SpanFactory.obtainNoExt(start, attrNameStyle))
                if (groups[5] == "=") {
                    val eqStart = start + groups[4].length
                    builder.add(lineIndex, SpanFactory.obtainNoExt(eqStart, operatorStyle))
                }
            }
            //双引号字符串
            else if (groups[6].isNotEmpty()) {
                val str = groups[6]
                //起始引号
                builder.add(lineIndex, SpanFactory.obtainNoExt(start, stringQuoteStyle))
                //内容
                if (str.length > 2) {
                    builder.add(lineIndex, SpanFactory.obtainNoExt(start + 1, attrValueStyle))
                }
                //结束引号
                builder.add(lineIndex, SpanFactory.obtainNoExt(start + str.length - 1, stringQuoteStyle))
            }
            //单引号字符串
            else if (groups[7].isNotEmpty()) {
                val str = groups[7]
                builder.add(lineIndex, SpanFactory.obtainNoExt(start, stringQuoteStyle))
                if (str.length > 2) {
                    builder.add(lineIndex, SpanFactory.obtainNoExt(start + 1, attrValueStyle))
                }
                builder.add(lineIndex, SpanFactory.obtainNoExt(start + str.length - 1, stringQuoteStyle))
            }
        }
    }

    override fun destroy() {
        super.destroy()
    }
}
