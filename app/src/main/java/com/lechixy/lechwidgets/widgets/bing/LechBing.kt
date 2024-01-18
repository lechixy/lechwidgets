package com.lechixy.lechwidgets.widgets.bing

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lechixy.lechwidgets.R
import com.lechixy.lechwidgets.common.BingUtil
import com.lechixy.lechwidgets.common.PinterestUtil
import com.lechixy.lechwidgets.common.Preferences
import com.lechixy.lechwidgets.database.Board
import com.lechixy.lechwidgets.database.BoardDatabase
import com.lechixy.lechwidgets.database.BoardPin
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class LechBing : AppWidgetProvider() {

    // Notification
    private val handler = Handler(Looper.getMainLooper())

    @OptIn(DelicateCoroutinesApi::class)
    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        createNotificationChannel(
            context,
            "bing_update",
            "Bing update notifications",
            "Notify you while updating bing widget",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationBuilder = NotificationCompat.Builder(context, "bing_update")
            .setSmallIcon(R.drawable.m3_celebration)
            .setContentTitle("Updating bing")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(Color.BLUE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(1, notificationBuilder.build())
        }

        //TODO
        val views = RemoteViews(context.packageName, R.layout.lech_bing)
        val appWidgetIds = getActiveWidgetIds(context)

        GlobalScope.launch(Dispatchers.IO) {
            val request: Request = Request.Builder()
                .url(BingUtil.bingTodayUrl)
                .build()
            val response = try {
                BingUtil.network.newCall(request).execute()
            } catch (e: Exception) {
                null
            }
            val queue = Volley.newRequestQueue(context)

            if (response != null && response.isSuccessful) {
                val responseBody = response.body!!.string()

                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val todaysImage =
                    BingUtil.bingUrl + jsonObject.get("images").asJsonArray.get(0).asJsonObject
                        .get("url").asString

                if(Preferences.todaysImage == todaysImage) return@launch

                val todaysTitle = jsonObject.get("images").asJsonArray.get(0).asJsonObject
                    .get("title").asString
                val copyright = jsonObject.get("images").asJsonArray.get(0).asJsonObject
                    .get("copyright").asString.split(" (")[0]
                val copyrightLink = jsonObject.get("images").asJsonArray.get(0).asJsonObject
                    .get("copyrightlink").asString

                Log.i("LECH", todaysTitle)

                val imageRequest = ImageRequest(
                    todaysImage,
                    { bingImage ->
                        Preferences.todaysImage = todaysImage
                        views.setImageViewBitmap(R.id.bingImage, bingImage)
                        val browserIntent = PendingIntent.getActivity(
                            context,
                            0,
                            Intent(Intent.ACTION_VIEW, Uri.parse(copyrightLink)),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.bingImage, browserIntent)
                        views.setTextViewText(R.id.bingTitle, todaysTitle)
                        views.setTextViewText(R.id.bingCopyright, copyright)
                        appWidgetManager.updateAppWidget(appWidgetIds, views)
                    },
                    0, 0, ImageView.ScaleType.CENTER, Bitmap.Config.ARGB_8888,
                    {
                        Log.i("LECH", "Can't get the image")
                    }
                )

                queue.add(imageRequest)
            }
        }

        handler.postDelayed(kotlinx.coroutines.Runnable {
            notificationManager.cancel(1)
        }, 1500)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleWidgetUpdates(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context);
        // Enter relevant functionality for when the first widget is created
        scheduleWidgetUpdates(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds);

        // We are setting todays image to empty cause
        Preferences.todaysImage = ""

        // reschedule update alarm so it does not include ID of currently removed widget
        scheduleWidgetUpdates(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

    private fun getActiveWidgetIds(context: Context): IntArray {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, this::class.java)

        // return ID of all active widgets within this AppWidgetProvider
        return appWidgetManager.getAppWidgetIds(componentName)
    }

    private fun scheduleWidgetUpdates(context: Context) {
        val activeWidgetIds = getActiveWidgetIds(context)

//        val next = context.alarmManager.nextAlarmClock
//        if (next != null) {
//            context.alarmManager.cancel(next.showIntent)
//        }

        if (activeWidgetIds.isNotEmpty()) {
            // Update Widget
            val widgetPendingIntent = getUpdatePendingIntent(context)

            // 21_600_000 ms is equals to 6 hours
            val time = (21_600_000).toLong()
            val initialDelay = SystemClock.elapsedRealtime() + time

            context.alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                initialDelay,
                time,
                widgetPendingIntent
            )
        }
    }

    private fun getUpdatePendingIntent(context: Context): PendingIntent {
        val widgetClass = this::class.java
        val widgetIds = getActiveWidgetIds(context)

        val updateIntent = Intent(context, widgetClass)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)

        val requestCode = widgetClass.name.hashCode()
        val flags = PendingIntent.FLAG_CANCEL_CURRENT or
                PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getBroadcast(context, requestCode, updateIntent, flags)
    }

    private val Context.alarmManager: AlarmManager
        get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun createNotificationChannel(
        context: Context,
        id: String,
        name: String,
        description: String,
        importance: Int
    ) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        val channel = NotificationChannel(id, name, importance).apply {
            description
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}