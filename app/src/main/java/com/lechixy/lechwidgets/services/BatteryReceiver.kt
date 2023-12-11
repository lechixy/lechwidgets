package com.lechixy.lechwidgets.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import com.lechixy.lechwidgets.common.GlanceUtil
import com.lechixy.lechwidgets.widgets.glance.LechGlance

class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val supportedIntents = arrayOf(
            Intent.ACTION_BATTERY_CHANGED, Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED, Intent.ACTION_BATTERY_LOW
        )

        if (supportedIntents.contains(intent.action)) {
            // Get the current battery level and scale
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPercentage = level / scale.toFloat() * 100
            val fullyCharged = batteryPercentage == 100f

            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val isPlugged =
                plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS

            val extras = Bundle()

            extras.putBoolean("fullyCharged", fullyCharged)
            extras.putBoolean("isPlugged", isPlugged)
            extras.putFloat("batteryPercentage", batteryPercentage)

            // Create an intent to send data to the widget provider
            val sendIntent = Intent(context, LechGlance::class.java)
            sendIntent.action = GlanceUtil.Events.UPDATE_BATTERY
            sendIntent.putExtras(extras)

            context.sendBroadcast(sendIntent)
        }
    }
}
