package com.lechixy.lechwidgets.common

import com.chibatching.kotpref.KotprefModel

object Preferences : KotprefModel() {
    override val commitAllPropertiesByDefault: Boolean = true

    // Glance
    var musicIconShape by stringPref(key = "musicIconShape", default = GlanceUtil.IconShapes.first())
    var colorfulMusicIcons by booleanPref(key = "colorfulMusicIcons", default = true)
    var colorfulNotificationIcons by booleanPref(key = "colorfulNotificationIcons", default = true)
    var notificationIconShape by stringPref(key = "notificationIconShape", default = GlanceUtil.IconShapes.first())

    var allowedNotificationCategories by stringPref(key = "allowedNotificationCategories", default = "")
    var notificationLastKey by stringPref(key = "notificationLastKey", default = "")
}