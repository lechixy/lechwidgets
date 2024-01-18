package com.lechixy.lechwidgets.widgets.glance

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
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.location.Location
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColor
import com.chibatching.kotpref.Kotpref
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lechixy.lechwidgets.R
import com.lechixy.lechwidgets.common.GlanceUtil
import com.lechixy.lechwidgets.common.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.DecimalFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class LechGlance : AppWidgetProvider() {

    // Location
    var client: FusedLocationProviderClient? = null

    // Network
    var network: OkHttpClient = OkHttpClient()

    // Notification
    private val handler = Handler(Looper.getMainLooper())
    private var delayedRunnable: Runnable? = null

    @OptIn(ExperimentalStdlibApi::class)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val views = RemoteViews(context.packageName, R.layout.lech_glance)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = getActiveWidgetIds(context)

        // Preferences
        Kotpref.init(context)

        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaperColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        var currentTheme =  GlanceUtil.ThemeColors.LIGHT

        if(Preferences.glanceTheme != GlanceUtil.GlanceThemeColors.AUTO){
            var preferColor = GlanceUtil.ThemeColors.LIGHT
            if(Preferences.glanceTheme == GlanceUtil.GlanceThemeColors.DARK){
                preferColor = GlanceUtil.ThemeColors.DARK
            }
            currentTheme = preferColor

            GlanceUtil.getAllTextsOnWidget().forEach {
                if(it.textType == GlanceUtil.GlanceTextType.TITLE){
                    views.setTextColor(it.idAtGlance, preferColor.titleColor)
                } else {
                    views.setTextColor(it.idAtGlance, preferColor.subtitleColor)
                }
            }

            GlanceUtil.getAllIconsOnWidget().forEach {
                var coloredDrawable =
                    ContextCompat.getDrawable(context, it.iconResource) as Drawable
                coloredDrawable = DrawableCompat.wrap(coloredDrawable)
                DrawableCompat.setTint(coloredDrawable, preferColor.titleColor)
                val bitmap = coloredDrawable.toBitmap()

                views.setImageViewBitmap(it.idAtGlance, bitmap)
            }
        } else {
            // TODO ADDING BACKGROUND COLOR SUPPORT
            // TODO ADDING WALLPAPER COLOR SUPPORT
            if (wallpaperColors != null) {
                val isWallpaperSupportsLight = GlanceUtil.isWallpaperSupportsLight(wallpaperColors.colorHints)
                val preferColor = GlanceUtil.getPreferColor(isWallpaperSupportsLight)
                currentTheme = preferColor

                GlanceUtil.getAllTextsOnWidget().forEach {
                    if(it.textType == GlanceUtil.GlanceTextType.TITLE){
                        views.setTextColor(it.idAtGlance, preferColor.titleColor)
                    } else {
                        views.setTextColor(it.idAtGlance, preferColor.subtitleColor)
                    }
                }

                GlanceUtil.getAllIconsOnWidget().forEach {
                    var coloredDrawable =
                        ContextCompat.getDrawable(context, it.iconResource) as Drawable
                    coloredDrawable = DrawableCompat.wrap(coloredDrawable)
                    DrawableCompat.setTint(coloredDrawable, preferColor.titleColor)
                    val bitmap = coloredDrawable.toBitmap()

                    views.setImageViewBitmap(it.idAtGlance, bitmap)
                }
            }
        }

        val event = GlanceUtil.getTodaysFirstEvent(context)

        if (event != null) {
            views.setViewVisibility(R.id.eventIcon, View.VISIBLE)
            var coloredDrawable =
                ContextCompat.getDrawable(context, R.drawable.m3_celebration) as Drawable
            coloredDrawable = DrawableCompat.wrap(coloredDrawable)

            val color = if(event.eventColor == 0){
                Color.WHITE
            } else event.eventColor

            DrawableCompat.setTint(coloredDrawable, color)
            val bitmap = coloredDrawable.toBitmap()

            views.setImageViewBitmap(R.id.eventIcon, bitmap)
            if(Preferences.colorfulEventName){
                views.setTextColor(R.id.todaysEvent, color)
            } else {
                views.setTextColor(R.id.todaysEvent, currentTheme.titleColor)
            }
            views.setTextViewText(R.id.todaysEvent, event.title)
        } else {
            views.setViewVisibility(R.id.eventIcon, View.GONE)
            views.setTextViewText(R.id.todaysEvent, context.getString(R.string.glance_noEvent))
        }

//        if(Preferences.textShadows){
//            GlanceUtil.getAllTextsOnWidget().forEach {
//                views.setInt(it, "setShadowLayer", 0)
//            }
//        } else {
//            GlanceUtil.getAllTextsOnWidget().forEach {
//                views.setFloat(it, "", 0f)
//            }
//        }

        appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)

        val extras = intent.extras ?: return

        if (intent.action.equals(GlanceUtil.Events.UPDATE_MUSIC)) {
            if(Preferences.isMusicEnabled.not()){
                views.setViewVisibility(R.id.musicContainer, View.GONE)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
                return
            }

            val session = extras.getParcelable(
                NotificationCompat.EXTRA_MEDIA_SESSION,
                MediaSession.Token::class.java
            )
            var currentMedia: MediaController? = null
            if (session != null) {
                currentMedia = MediaController(context, session)
            }

            val name = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)
            val author = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)
            val app = extras.getString("app")!!
            val icon = extras.getParcelable("appIcon", Icon::class.java) as Icon
            val title = "$name - $author"

            // If it's playing
            if (currentMedia != null && currentMedia.playbackState!!.state == PlaybackState.STATE_PLAYING) {

                val iconColor = GlanceUtil.getPlayerIconColor(app)

                views.setInt(
                    R.id.musicIcon,
                    "setBackgroundResource",
                    GlanceUtil.getNotificationIconShape(Preferences.musicIconShape)
                )
                if (Preferences.colorfulNotificationIcons) {
                    if (iconColor.toColor().luminance() < 0.65) {
                        icon.setTint(Color.WHITE)
                    } else {
                        icon.setTint(Color.BLACK)
                    }

                    views.setColorStateList(
                        R.id.musicIcon,
                        "setBackgroundTintList",
                        ColorStateList.valueOf(iconColor)
                    )
                } else {
                    icon.setTint(Color.BLACK)
                    views.setColorStateList(
                        R.id.musicIcon,
                        "setBackgroundTintList",
                        ColorStateList.valueOf(Color.WHITE)
                    )
                }

                views.setImageViewIcon(R.id.musicIcon, icon)
                views.setTextViewText(R.id.music, title)
                views.setViewVisibility(R.id.musicContainer, View.VISIBLE)

                val musicIntent = PendingIntent.getActivity(
                    context,
                    0,
                    context.packageManager.getLaunchIntentForPackage(app),
                    PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.musicContainer, musicIntent);
            } else {
                views.setViewVisibility(R.id.musicContainer, View.GONE)
            }

            appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)

        }
        if (intent.action.equals(GlanceUtil.Events.NEW_NOTIFICATION)) {
            if(Preferences.isNotificationsEnabled.not()){
                views.setViewVisibility(R.id.notificationContainer, View.GONE)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
                return
            }

            val title = extras.getString(NotificationCompat.EXTRA_TITLE)
            val app = extras.getString("app")

            views.setTextViewText(R.id.notification, title)

            // Sometimes notifications can be send by android so we prevent that to happen
            try {
                views.setOnClickPendingIntent(
                    R.id.notificationContainer, PendingIntent.getActivity(
                        context,
                        0,
                        context.packageManager.getLaunchIntentForPackage(app!!),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } catch (_: Exception) {
            }

            val key = extras.getString("key")!!
            Preferences.notificationLastKey = key

            val category = extras.getString("category")
            if (Preferences.allowedNotificationCategories.isNotBlank() &&
                category.isNullOrBlank().not() &&
                Preferences.allowedNotificationCategories.split(",").contains(category).not()
            ) return;

            val iconColor = extras.getInt("iconColor")
            val icon =
                extras.getParcelable(NotificationCompat.EXTRA_SMALL_ICON, Icon::class.java) as Icon

            views.setInt(
                R.id.notificationIcon,
                "setBackgroundResource",
                GlanceUtil.getNotificationIconShape(Preferences.notificationIconShape)
            )
            if (Preferences.colorfulNotificationIcons && iconColor != 0) {
                if (iconColor.toColor().luminance() < 0.5) {
                    icon.setTint(Color.WHITE)
                } else {
                    icon.setTint(Color.BLACK)
                }

                views.setColorStateList(
                    R.id.notificationIcon,
                    "setBackgroundTintList",
                    ColorStateList.valueOf(iconColor)
                )
            } else {
                icon.setTint(Color.BLACK)
                views.setColorStateList(
                    R.id.notificationIcon,
                    "setBackgroundTintList",
                    ColorStateList.valueOf(Color.WHITE)
                )
            }
            views.setImageViewIcon(R.id.notificationIcon, icon)

            if (delayedRunnable != null) {
                handler.removeCallbacks(delayedRunnable!!)
            }

            delayedRunnable = Runnable {
                delayedRunnable = null
                Preferences.notificationLastKey = ""

                views.setViewVisibility(R.id.notificationContainer, View.GONE)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
            }

            handler.postDelayed(delayedRunnable!!, 60 * 1000)

            views.setViewVisibility(R.id.notificationContainer, View.VISIBLE)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)

        }
        if (intent.action.equals(GlanceUtil.Events.REMOVE_NOTIFICATION)) {
            if(Preferences.isNotificationsEnabled.not()){
                views.setViewVisibility(R.id.notificationContainer, View.GONE)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
                return
            }

            //val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString()
            val key = extras.getCharSequence("key").toString()

            if (Preferences.notificationLastKey == key) {
                delayedRunnable = null
                Preferences.notificationLastKey = ""

                views.setViewVisibility(R.id.notificationContainer, View.GONE)

                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
            }

        }
        if (intent.action.equals(GlanceUtil.Events.UPDATE_BATTERY)) {
            if(Preferences.isBatteryEnabled.not()){
                views.setViewVisibility(R.id.batteryContainer, View.GONE)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
                return
            }

            val isPlugged = extras.getBoolean("isPlugged")
            val fullyCharged = extras.getBoolean("fullyCharged")
            val batteryPercentage = extras.getFloat("batteryPercentage")

            if (fullyCharged && isPlugged) {
                views.setTextViewText(R.id.battery, "Fully charged")
                views.setTextColor(R.id.battery, GlanceUtil.Colors.BATTERY_FULL)

                var coloredDrawable =
                    ContextCompat.getDrawable(context, R.drawable.batteryfull)  as Drawable
                coloredDrawable = DrawableCompat.wrap(coloredDrawable)
                DrawableCompat.setTint(coloredDrawable, GlanceUtil.Colors.BATTERY_FULL)
                val bitmap = coloredDrawable.toBitmap()
                views.setImageViewBitmap(R.id.batteryIcon, bitmap)
            } else if (isPlugged) {

                views.setTextColor(R.id.battery, currentTheme.titleColor)
                views.setTextViewText(R.id.battery, "Charging ${batteryPercentage.toInt()}%")
                val chargingIcon = GlanceUtil.getBatteryIcon(batteryPercentage.toInt())
                var coloredDrawable =
                    ContextCompat.getDrawable(context, chargingIcon) as Drawable
                coloredDrawable = DrawableCompat.wrap(coloredDrawable)
                DrawableCompat.setTint(coloredDrawable, currentTheme.titleColor)
                val bitmap = coloredDrawable.toBitmap()

                views.setImageViewBitmap(R.id.batteryIcon, bitmap)
            } else if (batteryPercentage.toInt() <= 20) {
                views.setTextViewText(R.id.battery, "Charge your phone")
                views.setTextColor(R.id.battery, GlanceUtil.Colors.BATTERY_LOW)

                var coloredDrawable =
                    ContextCompat.getDrawable(context, R.drawable.batterylow) as Drawable
                coloredDrawable = DrawableCompat.wrap(coloredDrawable)
                DrawableCompat.setTint(coloredDrawable, GlanceUtil.Colors.BATTERY_LOW)
                val bitmap = coloredDrawable.toBitmap()

                views.setImageViewBitmap(R.id.batteryIcon, bitmap)
            } else {
                views.setViewVisibility(R.id.batteryContainer, View.GONE)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
                return
            }

            views.setViewVisibility(R.id.batteryContainer, View.VISIBLE)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
        }
    }

    @SuppressLint("InlinedApi")
    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // This needs to work for once immediately cause widget needs to be updated
        client = LocationServices.getFusedLocationProviderClient(context);

        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.lech_glance)

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            client?.lastLocation?.addOnSuccessListener { location ->
                Log.i("LocationService", "lastLocation")
                if (location != null) {
                    Log.i("LocationService", "gettedLocation")

                    val currentTime = LocalTime.now()
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    val formattedTime = currentTime.format(formatter)
                    views.setTextViewText(R.id.lastUpdate, "$formattedTime update")

                    // Instruct the widget manager to update the widget
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)

                    getWeather(context, location, formattedTime)
                }
            }
        }
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
        scheduleUpdates(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context);
        // Enter relevant functionality for when the first widget is created
        scheduleUpdates(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds);
        // reschedule update alarm so it does not include ID of currently removed widget
        scheduleUpdates(context)
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

    private fun scheduleUpdates(context: Context) {
        val activeWidgetIds = getActiveWidgetIds(context)

        val next = context.alarmManager.nextAlarmClock
        if (next != null) {
            context.alarmManager.cancel(next.showIntent)
        }

        if (activeWidgetIds.isNotEmpty()) {
            val pendingIntent = getUpdatePendingIntent(context)

            val twentyMin = (1_200_000).toLong()
            val initialDelay = SystemClock.elapsedRealtime() + twentyMin

            context.alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                initialDelay,
                twentyMin,
                pendingIntent
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
        val channel = NotificationChannel(id, name, importance)
        channel.description = description
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun getWeather(context: Context, location: Location, formattedTime: String) {
        createNotificationChannel(
            context,
            "weather",
            "Weather update notifications",
            "Notify you while updating weather",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationBuilder = NotificationCompat.Builder(context, "weather")
            .setSmallIcon(R.drawable.m3_location)
            .setContentTitle("Updating weather")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setColor(Color.YELLOW)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(1, notificationBuilder.build())
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.lech_glance)

        val apiFormat = DecimalFormat("#.00")
        val latitude = apiFormat.format(location.latitude)
        val longitude = apiFormat.format(location.longitude)

        val calendarIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
            PendingIntent.FLAG_IMMUTABLE
        )
        val weatherIntent = PendingIntent.getActivity(
            context,
            0,
            context.packageManager.getLaunchIntentForPackage("com.sec.android.daemonapp"),
            PendingIntent.FLAG_IMMUTABLE
        )
        val clockIntent = PendingIntent.getActivity(
            context,
            0,
            context.packageManager.getLaunchIntentForPackage("com.sec.android.app.clockpackage"),
            PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.dateContainer, calendarIntent);
        views.setOnClickPendingIntent(R.id.weatherContainer, weatherIntent);
        views.setOnClickPendingIntent(R.id.clock, clockIntent)

        GlobalScope.launch(Dispatchers.IO) {
            val url =
                "https://api.openweathermap.org/data/2.5/weather?lat=${latitude}&lon=${longitude}&appid=4c98cf6f8135638d0807ac9abf7ebbdb&units=metric"
            val request: Request = Request.Builder()
                .url(url)
                .build()
            val response = try {
                network.newCall(request).execute()
            } catch (e: Exception) {
                null
            }

            if (response != null && response.isSuccessful) {
                val responseBody = response.body!!.string()

                views.setTextViewText(R.id.lastUpdate, "$formattedTime weather update")

                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val weatherId =
                    jsonObject.get("weather").asJsonArray.get(0).asJsonObject.get("id").asInt
                // We multiply with 1000 cause we need milliseconds
                val sunrise = jsonObject.get("sys").asJsonObject.get("sunrise").asLong
                val sunset = jsonObject.get("sys").asJsonObject.get("sunset").asLong

                val dayOrNight = GlanceUtil.getItsDayOrNight(sunrise, sunset);
                val weather = GlanceUtil.getWeather(weatherId, dayOrNight)
                views.setImageViewResource(R.id.weatherIcon, weather.iconName)
                views.setTextViewText(R.id.weatherDescription, weather.weatherDescription)

                val rawTemp = jsonObject.get("main").asJsonObject.get("temp").asFloat
                val showFormat = DecimalFormat("0.0")
                val temp = showFormat.format(rawTemp)
                views.setTextViewText(R.id.weatherTemp, "$temp°C")

                val location = jsonObject.get("name").asString
                views.setTextViewText(R.id.location, location)

            } else {
                views.setTextViewText(R.id.lastUpdate, "$formattedTime can't update")
            }

            handler.postDelayed(Runnable {
                notificationManager.cancel(1)
            }, 1500)

            val activeWidgetIds = getActiveWidgetIds(context)
            appWidgetManager.updateAppWidget(activeWidgetIds, views)
        }
    }
}