package com.lechixy.lechwidgets

import android.content.Intent
import android.graphics.drawable.Icon
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat


class NotificationListener : NotificationListenerService() {

    private val supportedApps = arrayOf(
        "com.spotify.music", "com.google.android.youtube",
        "com.google.android.apps.youtube.music"
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
            // Needed additional info
            notificationExtras.putCharSequence("app", sbn.packageName)
            notificationExtras.putParcelable("appIcon", sbn.notification.smallIcon)

            // Create an intent to send data to the widget provider
            val intent = Intent(this, LechGlance::class.java)
            intent.action = "com.lechixy.lechwidgets.UPDATE_MUSIC"
            intent.putExtras(notificationExtras)

            sendBroadcast(intent)
        } else if (!ignoredApps.contains(sbn.packageName)) {
            val infoBundle = Bundle()

            // Needed additional info
            infoBundle.putCharSequence("app", sbn.packageName)
            val title = notificationExtras.getCharSequence(NotificationCompat.EXTRA_TITLE)
            infoBundle.putCharSequence(NotificationCompat.EXTRA_TITLE, title)
            infoBundle.putParcelable(
                NotificationCompat.EXTRA_SMALL_ICON,
                sbn.notification.smallIcon
            )
            infoBundle.putCharSequence("key", sbn.key)

            // Create an intent to send data to the widget provider
            val intent = Intent(this, LechGlance::class.java)
            intent.action = "com.lechixy.lechwidgets.NEW_NOTIFICATION"
            intent.putExtras(infoBundle)

            sendBroadcast(intent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)

        // Extract data from the notification (e.g., title, text, extras)
        val notificationExtras = sbn.notification.extras

        if (!ignoredApps.contains(sbn.packageName)) {
            val infoBundle = Bundle()

            // Needed additional info
            val title = notificationExtras.getCharSequence(NotificationCompat.EXTRA_TITLE)
            infoBundle.putCharSequence(NotificationCompat.EXTRA_TITLE, title)
            infoBundle.putCharSequence("key", sbn.key)

            // Create an intent to send data to the widget provider
            val intent = Intent(this, LechGlance::class.java)
            intent.action = "com.lechixy.lechwidgets.REMOVE_NOTIFICATION"
            intent.putExtras(infoBundle)

            // If this is a media remove
            if (supportedApps.contains(sbn.packageName) &&
                notificationExtras != null &&
                notificationExtras.containsKey(NotificationCompat.EXTRA_MEDIA_SESSION)
            ) {
                intent.action = "com.lechixy.lechwidgets.UPDATE_MUSIC"
            }

            sendBroadcast(intent)
        }
    }
}
