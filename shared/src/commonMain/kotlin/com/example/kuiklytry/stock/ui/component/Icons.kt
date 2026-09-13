package com.example.kuiklytry.stock.ui.component

/**
 * 应用图标抽象。
 *
 * 当前 Demo 使用跨端稳定的 Unicode 字形作为图标（避免额外引入图片/字体资源，开箱即用）。
 * 后续可无缝替换为 Kuikly 官方图标组件或 iconfont：
 * 将这里引用到的常量改由图标组件渲染即可，业务层无需改动。
 */
internal object AppIcons {

    /** 返回箭头 */
    const val BACK = "‹"

    /** 汉堡菜单 */
    const val MENU = "≡"

    /** 发送 */
    const val SEND = "➤"

    /** 上涨三角 */
    const val UP = "▲"

    /** 下跌三角 */
    const val DOWN = "▼"

    /** AI 助手头像文字 */
    const val AI_AVATAR = "AI"

    /** 用户头像文字 */
    const val USER_AVATAR = "我"
}
