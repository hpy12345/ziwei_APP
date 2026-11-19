package com.ziweistar.app.ui.nav

/**
 * 全局路由表。所有界面跳转必须使用这里的常量。
 */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARD_NATAL = "onboard/natal"
    const val ONBOARD_PACT = "onboard/pact"
    const val ONBOARD_PHYSIQUE = "onboard/physique"
    const val HOME = "home"
    const val TIMER = "timer"
    const val CALENDAR = "calendar"
    const val REPORT = "report"
    const val SETTINGS = "settings"
    const val GLOSSARY = "glossary"

    /** 收功后的「入定」十五分钟温养屏（设计文档 3.3） */
    const val IMMERSION = "immersion"

    /** 参数：periodKey（2026-W46 / 2026-11） */
    const val REPORT_ARG = "periodKey"
    const val REPORT_WITH_ARG = "report?periodKey={periodKey}"
}
