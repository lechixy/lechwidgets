package com.lechixy.lechwidgets.widgets.bing

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LightingColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lechixy.lechwidgets.R
import com.lechixy.lechwidgets.common.BingUtil
import com.lechixy.lechwidgets.common.GlanceUtil
import com.lechixy.lechwidgets.common.GoogleUtil
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

class LechGoogle : AppWidgetProvider() {

    // Notification
    private val handler = Handler(Looper.getMainLooper())
    @RequiresApi(Build.VERSION_CODES.S)
    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        createNotificationChannel(
            context,
            "google_update",
            "Google update notifications",
            "Notify you while updating google widget",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationBuilder = NotificationCompat.Builder(context, "google_update")
            .setSmallIcon(R.drawable.google)
            .setContentTitle("Updating google")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(Color.BLUE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        val notificationManager = NotificationManagerCompat.from(context)
//        if (ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.POST_NOTIFICATIONS
//            ) == PackageManager.PERMISSION_GRANTED
//        ) {
//            notificationManager.notify(1, notificationBuilder.build())
//        }

        val views = RemoteViews(context.packageName, R.layout.lech_google)
        val appWidgetIds = getActiveWidgetIds(context)

        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaperColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)

        // Widget Theme
        if(Preferences.googleWidgetTheme != GoogleUtil.ThemeColors.AUTO) {
            var preferColor = GoogleUtil.Colors.LIGHT
            if(Preferences.googleWidgetTheme == GoogleUtil.ThemeColors.DARK){
                preferColor = GoogleUtil.Colors.DARK
            }

            views.setColorStateList(
                R.id.searchBar,
                "setBackgroundTintList",
                ColorStateList.valueOf(preferColor)
            )
        } else {
            if (wallpaperColors != null){
                val isWallpaperSupportsLight = GlanceUtil.isWallpaperSupportsLight(wallpaperColors.colorHints)
                var preferColor = GoogleUtil.Colors.LIGHT
                if(isWallpaperSupportsLight.not()){
                    preferColor = GoogleUtil.Colors.DARK
                }

                views.setColorStateList(
                    R.id.searchBar,
                    "setBackgroundTintList",
                    ColorStateList.valueOf(preferColor)
                )
            }
        }

        val googleSearchIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.googlequicksearchbox.SearchActivity"
                )
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.searchBar, googleSearchIntent)

        val googleAppIntent = PendingIntent.getActivity(
            context,
            0,
            context.packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox"),
            PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.googleImage, googleAppIntent)

        if(wallpaperColors != null && Preferences.wallpaperColorsApplyIcons){
            val googleIconDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.google) as Drawable).apply {
                setTint(wallpaperColors.primaryColor.toArgb())
                setTintBlendMode(BlendMode.HUE)
            }
            val googleIcon = GoogleUtil.findAllThisColoredPixelsAndClear(
                wallpaperColors.primaryColor.toArgb(), googleIconDrawable.toBitmap()
            )
            views.setImageViewBitmap(R.id.googleImage, googleIcon)
        } else {
            views.setImageViewResource(R.id.googleImage, R.drawable.google)
        }

        val googleMicIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_SEARCH_LONG_PRESS),
            PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.micImage, googleMicIntent)

        if(wallpaperColors != null && Preferences.wallpaperColorsApplyIcons){
            val googleMicDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.mic) as Drawable).apply {
                setTint(wallpaperColors.primaryColor.toArgb())
                setTintBlendMode(BlendMode.HUE)
            }
            val micIcon = GoogleUtil.findAllThisColoredPixelsAndClear(
                wallpaperColors.primaryColor.toArgb(), googleMicDrawable.toBitmap()
            )
            views.setImageViewBitmap(R.id.micImage, micIcon)
        } else {
            views.setImageViewResource(R.id.micImage, R.drawable.mic)
        }

        val googleLensIntent = PendingIntent.getActivity(
            context,
            0,
            context.packageManager.getLaunchIntentForPackage("com.google.ar.lens"),
            PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.lensImage, googleLensIntent)

        if(wallpaperColors != null && Preferences.wallpaperColorsApplyIcons){
            val googleLensDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.lens) as Drawable).apply {
                setTint(wallpaperColors.primaryColor.toArgb())
                setTintBlendMode(BlendMode.HUE)
            }
            val lensIcon = GoogleUtil.findAllThisColoredPixelsAndClear(
                wallpaperColors.primaryColor.toArgb(), googleLensDrawable.toBitmap()
            )
            views.setImageViewBitmap(R.id.lensImage, lensIcon)
        } else {
            views.setImageViewResource(R.id.lensImage, R.drawable.lens)
        }

        appWidgetManager.updateAppWidget(appWidgetIds, views)

//        handler.postDelayed(kotlinx.coroutines.Runnable {
//            notificationManager.cancel(1)
//        }, 1500)
    }

    @SuppressLint("NewApi")
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