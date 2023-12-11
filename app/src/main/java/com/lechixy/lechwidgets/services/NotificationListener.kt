package com.lechixy.lechwidgets.services

import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColor
import com.lechixy.lechwidgets.common.GlanceUtil
import com.lechixy.lechwidgets.widgets.glance.LechGlance


class NotificationListener : NotificationListenerService() {

    private val supportedApps = arrayOf(
        "com.spotify.music", "com.google.android.youtube",
        "com.google.android.apps.youtube.music", "com.apple.android.music"
    )
    private val ignoredApps = arrayOf("android", "com.android.systemui")

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        // Extract data from the notification (e.g., title, text, extras)
        val notificationExtras = sbn.notification.extras

        if (supportedApps.contains(sbn.packageName) && notificationExtras != null && notificationExtras.containsKey(
                NotificationCompat.EXTRA_MEDIA_SESSION
            )
        ) {
            sendUpdateMusic(sbn)
        } else {
            val infoBundle = Bundle()

            // Needed additional info
            infoBundle.putString("app", sbn.packageName)

            val title = notificationExtras.getCharSequence(NotificationCompat.EXTRA_TITLE)
            if (title.isNullOrEmpty()) return;
            infoBundle.putString(NotificationCompat.EXTRA_TITLE, title.toString())

            val category = sbn.notification.category
            infoBundle.putString("category", category)

            val iconColor = sbn.notification.color
            infoBundle.putInt("iconColor", iconColor)

            infoBundle.putParcelable(
                NotificationCompat.EXTRA_SMALL_ICON,
                sbn.notification.smallIcon
            )
            infoBundle.putString("key", sbn.key)

            // Create an intent to send data to the widget provider
            val intent = Intent(this, LechGlance::class.java)
            intent.action = GlanceUtil.Events.NEW_NOTIFICATION
            intent.putExtras(infoBundle)

            sendBroadcast(intent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)

        // Extract data from the notification (e.g., title, text, extras)
        val notificationExtras = sbn.notification.extras

        if(supportedApps.contains(sbn.packageName)){
            sendUpdateMusic(sbn)
        } else if (!ignoredApps.contains(sbn.packageName)) {
            val infoBundle = Bundle()

            // Needed additional info
            val title = notificationExtras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString()
            infoBundle.putString(NotificationCompat.EXTRA_TITLE, title)
            infoBundle.putString("key", sbn.key)

            // Create an intent to send data to the widget provider
            val intent = Intent(this, LechGlance::class.java)
            intent.action = GlanceUtil.Events.REMOVE_NOTIFICATION
            intent.putExtras(infoBundle)

            // If this is a media remove
            if (supportedApps.contains(sbn.packageName) &&
                notificationExtras != null &&
                notificationExtras.containsKey(NotificationCompat.EXTRA_MEDIA_SESSION)
            ) {
                intent.action = GlanceUtil.Events.UPDATE_MUSIC
            }

            sendBroadcast(intent)
        }
    }

    fun sendUpdateMusic(sbn: StatusBarNotification){
        val notificationExtras = sbn.notification.extras

        // Needed additional info
        notificationExtras.putString("app", sbn.packageName)
        notificationExtras.putParcelable("appIcon", sbn.notification.smallIcon)
        val title = notificationExtras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString()
        // We are replacing explicit icon with blank cause it changes size of text line
        notificationExtras.putCharSequence(NotificationCompat.EXTRA_TITLE, title.replace(" 🅴", ""))

        // Create an intent to send data to the widget provider
        val intent = Intent(this, LechGlance::class.java)
        intent.action = GlanceUtil.Events.UPDATE_MUSIC
        intent.putExtras(notificationExtras)

        sendBroadcast(intent)
    }
}
