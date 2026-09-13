package com.example.kuiklytry.stock.ui.theme

import com.tencent.kuikly.core.base.Color

/**
 * 全局主题色板与常用尺寸。
 *
 * 集中管理颜色与间距，保证视觉风格统一、便于后续换肤。
 */
internal object StockTheme {

    // ---------- 品牌 / 功能色 ----------
    /** AI 品牌主色（蓝色） */
    val primary: Color = Color(0xFF2F6BFF)

    /** 主色浅色背景 */
    val primaryLight: Color = Color(0xFFEAF1FF)

    /** A 股上涨红 */
    val up: Color = Color(0xFFE34D59)

    /** A 股下跌绿 */
    val down: Color = Color(0xFF00A870)

    // ---------- 中性色 ----------
    /** 页面背景 */
    val background: Color = Color(0xFFF4F6F9)

    /** 卡片 / 气泡背景 */
    val surface: Color = Color.WHITE

    /** 主要文字 */
    val textPrimary: Color = Color(0xFF1F2329)

    /** 次要文字 */
    val textSecondary: Color = Color(0xFF8A919F)

    /** 分隔线 */
    val divider: Color = Color(0xFFECEEF2)

    // ---------- 气泡 ----------
    /** 用户气泡背景 */
    val bubbleUser: Color = primary

    /** AI 气泡背景 */
    val bubbleAssistant: Color = Color.WHITE

    /** 用户气泡文字 */
    val bubbleUserText: Color = Color.WHITE

    // ---------- 常用间距 ----------
    const val SPACE_XS = 4f
    const val SPACE_SM = 8f
    const val SPACE_MD = 12f
    const val SPACE_LG = 16f
    const val SPACE_XL = 20f

    // ---------- 圆角 ----------
    const val RADIUS_SM = 8f
    const val RADIUS_MD = 12f
    const val RADIUS_LG = 16f
    const val RADIUS_XL = 20f
}
