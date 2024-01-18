package com.lechixy.lechwidgets.common

import com.chibatching.kotpref.KotprefModel

object Preferences : KotprefModel() {
    override val commitAllPropertiesByDefault: Boolean = true

    // Google
    var googleWidgetTheme by stringPref(key = "googleWidgetTheme", default = "Auto")
    var wallpaperColorsApplyIcons by booleanPref(key = "wallpaperColorsApplyIcons", default = true)

    // Bing
    var todaysImage by stringPref(key = "todaysImage", default = "")

    // Glance
    var glanceTheme by stringPref(key = "glanceTheme", default = "Auto")

    var isMusicEnabled by booleanPref(key = "isMusicEnabled", default = true)
    var musicIconShape by stringPref(key = "musicIconShape", default = GlanceUtil.IconShapes.first())
    var colorfulMusicIcons by booleanPref(key = "colorfulMusicIcons", default = true)

    var isNotificationsEnabled by booleanPref(key = "isNotificationsEnabled", default = true)
    var allowedNotificationCategories by stringPref(key = "allowedNotificationCategories", default = "")
    var notificationLastKey by stringPref(key = "notificationLastKey", default = "")
    var colorfulNotificationIcons by booleanPref(key = "colorfulNotificationIcons", default = true)
    var notificationIconShape by stringPref(key = "notificationIconShape", default = GlanceUtil.IconShapes.first())

    var isBatteryEnabled by booleanPref(key = "isBatteryEnabled", default = true)

    var colorfulEventName by booleanPref(key = "colorfulEventName", default = true)
}