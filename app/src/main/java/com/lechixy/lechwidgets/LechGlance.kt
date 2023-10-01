package com.lechixy.lechwidgets

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.text.DecimalFormat
import java.time.Instant
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

    // Battery
    private var batteryReceiver: BatteryReceiver? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val views = RemoteViews(context.packageName, R.layout.lech_glance)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = getActiveWidgetIds(context)
        val prefs = context.getSharedPreferences("LechGlance", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val extras = intent.extras ?: return

        if (intent.action.equals("com.lechixy.lechwidgets.UPDATE_MUSIC")) {
            // Extract data from the bundle and update your widget views
            // Update your app widget UI elements using RemoteViews
            // ...
            Log.i("Broadcast", "Received data")

            val session = extras.getParcelable(
                NotificationCompat.EXTRA_MEDIA_SESSION,
                MediaSession.Token::class.java
            )
            val newSession = MediaController(context, session!!)

            val name = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)
            val author = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)
            val app = extras.getCharSequence("app").toString()
            var icon = extras.getParcelable("appIcon", Icon::class.java) as Icon
            val title = "$name - $author"

            // If it's playing
            if (newSession.playbackState!!.state == PlaybackState.STATE_PLAYING) {

                val toggleColorfulMusicIcons = prefs.getBoolean("toggleColorfulMusicIcons", true)

                if (toggleColorfulMusicIcons) {
                    if (app == "com.spotify.music") {
                        icon = icon.setTint(Color.rgb(30, 215, 96))
                    }
                    if (app == "com.google.android.youtube" || app == "com.google.android.apps.youtube.music") {
                        icon = icon.setTint(Color.rgb(255, 0, 0))
                    }
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
        if (intent.action.equals("com.lechixy.lechwidgets.NEW_NOTIFICATION")) {
            val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString()
            val icon =
                extras.getParcelable(NotificationCompat.EXTRA_SMALL_ICON, Icon::class.java) as Icon
            val app = extras.getCharSequence("app").toString()
            if (title.isNullOrEmpty()) return;

            val key = extras.getCharSequence("key").toString()
            editor.putString("lastKey", key)
            editor.apply()

            views.setViewVisibility(R.id.notificationContainer, View.VISIBLE)

            icon.setTint(Color.WHITE)
            views.setImageViewIcon(R.id.notificationIcon, icon)

            views.setTextViewText(R.id.notification, title)
            views.setOnClickPendingIntent(
                R.id.notificationContainer, PendingIntent.getActivity(
                    context,
                    0,
                    context.packageManager.getLaunchIntentForPackage(app),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

            if (delayedRunnable != null) {
                handler.removeCallbacks(delayedRunnable!!)
            }

            delayedRunnable = Runnable {
                delayedRunnable = null
                editor.remove("lastKey")
                editor.apply()

                views.setViewVisibility(R.id.notificationContainer, View.GONE)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
            }

            handler.postDelayed(delayedRunnable!!, 60 * 1000)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)

        }
        if (intent.action.equals("com.lechixy.lechwidgets.REMOVE_NOTIFICATION")) {
            val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString()
            val key = extras.getCharSequence("key").toString()

            val lastNotificationKey = prefs.getString("lastKey", null)

            if (lastNotificationKey.equals(key)) {
                delayedRunnable = null
                editor.remove("lastKey")
                editor.apply()

                views.setViewVisibility(R.id.notificationContainer, View.GONE)

                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
            }

        }
        if (intent.action.equals("com.lechixy.lechwidgets.UPDATE_BATTERY")) {
            val isPlugged = extras.getBoolean("isPlugged")
            val fullyCharged = extras.getBoolean("fullyCharged")
            val batteryPercentage = extras.getFloat("batteryPercentage")

            var haveChange = false

            if (fullyCharged && isPlugged) {
                haveChange = true
                views.setTextViewText(R.id.battery, "Fully charged")
                var coloredDrawable =
                    ContextCompat.getDrawable(context, R.drawable.batteryfull)
                coloredDrawable = DrawableCompat.wrap(coloredDrawable!!)
                DrawableCompat.setTint(coloredDrawable, Color.rgb(0, 255, 128))
                val bitmap = coloredDrawable.toBitmap()

                views.setImageViewBitmap(R.id.batteryIcon, bitmap)
            } else if (isPlugged) {
                haveChange = true
                views.setTextViewText(R.id.battery, "Charging ${batteryPercentage.toInt()}%")
                val chargingIcon = getBatteryIcon(batteryPercentage.toInt())
                views.setImageViewResource(R.id.batteryIcon, chargingIcon)
            } else if (batteryPercentage.toInt() <= 20) {
                haveChange = true
                views.setTextViewText(R.id.battery, "Charge your phone")

                var coloredDrawable =
                    ContextCompat.getDrawable(context, R.drawable.batterylow)
                coloredDrawable = DrawableCompat.wrap(coloredDrawable!!)
                DrawableCompat.setTint(coloredDrawable, Color.rgb(229, 62, 53))
                val bitmapDrawable = coloredDrawable
                val bitmap = bitmapDrawable.toBitmap()

                views.setImageViewBitmap(R.id.batteryIcon, bitmap)
            } else {
                views.setViewVisibility(R.id.batteryContainer, View.GONE)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
            }

            if (haveChange) {
                views.setViewVisibility(R.id.batteryContainer, View.VISIBLE)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
            }
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
        batteryReceiver = BatteryReceiver()
        context.applicationContext.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        );
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds);
        // reschedule update alarm so it does not include ID of currently removed widget
        scheduleUpdates(context)
        if (batteryReceiver != null) {
            context.applicationContext.unregisterReceiver(batteryReceiver);
        }
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

    private fun getWeather(context: Context, location: Location, formattedTime: String) {
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
            val response: Response = network.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body!!.string()

                views.setTextViewText(R.id.lastUpdate, "$formattedTime weather update")

                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val weatherId =
                    jsonObject.get("weather").asJsonArray.get(0).asJsonObject.get("id").asInt
                // We multiply with 1000 cause we need milliseconds
                val sunrise = jsonObject.get("sys").asJsonObject.get("sunrise").asLong
                val sunset = jsonObject.get("sys").asJsonObject.get("sunset").asLong

                val dayOrNight = getItsDayOrNight(sunrise, sunset);
                val iconName = getWeatherIcon(weatherId, dayOrNight)
                views.setImageViewResource(R.id.weatherIcon, iconName)

                val rawTemp = jsonObject.get("main").asJsonObject.get("temp").asFloat
                val showFormat = DecimalFormat("#.0")
                val temp = showFormat.format(rawTemp)
                views.setTextViewText(R.id.weatherTemp, "$temp°C")

                val location = jsonObject.get("name").asString
                views.setTextViewText(R.id.location, location)

            } else {
                views.setTextViewText(R.id.lastUpdate, "$formattedTime can't update")
            }

            val activeWidgetIds = getActiveWidgetIds(context)
            appWidgetManager.updateAppWidget(activeWidgetIds, views)
        }
    }
}

fun getItsDayOrNight(sunrise: Long, sunset: Long): String {
    val currentMil = System.currentTimeMillis()
    val currentSec = Instant.ofEpochMilli(currentMil).epochSecond

    return if (currentSec in sunrise..sunset) {
        Log.i("Time", "It's daytime")
        "day"
    } else {
        Log.i("Time", "It's night")
        "night"
    }
}

fun getWeatherIcon(weatherId: Int, dayOrNight: String): Int {
    var iconName: Int

    // Clear Sky
    if (weatherId == 800 && dayOrNight == "day") {
        iconName = R.drawable.w11_sun
    } else {
        iconName = R.drawable.w11_moon
    }

    // Partly Cloudy
    if (weatherId in 801..804 && dayOrNight == "day") {
        when (weatherId) {
            801 -> iconName = R.drawable.w11_day_partly_cloudy;
            802 -> iconName = R.drawable.w11_day_mostly_cloudy;
            803 -> iconName = R.drawable.w11_day_full_cloudy;
            804 -> iconName = R.drawable.w11_day_full_cloudy;
        }
    } else {
        when (weatherId) {
            801 -> iconName = R.drawable.w11_night_partly_cloudy;
            802 -> iconName = R.drawable.w11_night_partly_cloudy;
            803 -> iconName = R.drawable.w11_night_mostly_cloudy;
            804 -> iconName = R.drawable.w11_night_mostly_cloudy;
        }
    }

    // Atmosphere
    if (weatherId in 701..781) {
        when (weatherId) {
            701 -> iconName = R.drawable.w11_fog;
            711 -> iconName = R.drawable.w11_fog;
            721 -> iconName = R.drawable.w11_haze;
            731 -> iconName = R.drawable.w11_dust;
            741 -> iconName = R.drawable.w11_fog;
            751 -> iconName = R.drawable.w11_dust;
            761 -> iconName = R.drawable.w11_dust;
            762 -> iconName = R.drawable.w11_dust;
            771 -> iconName = R.drawable.w11_hurricane;
            781 -> iconName = R.drawable.w11_tornado;
        }
    }
    // Snow
    if (weatherId in 600..622) {
        when (weatherId) {
            600 -> iconName = R.drawable.w11_light_snow;
            601 -> iconName = R.drawable.w11_snow;
            602 -> iconName = R.drawable.w11_snow_storm;
            611 -> iconName = R.drawable.w11_sleet;
            612 -> iconName = R.drawable.w11_sleet;
            613 -> iconName = R.drawable.w11_sleet;
            615 -> iconName = R.drawable.w11_sleet;
            616 -> iconName = R.drawable.w11_sleet;
            620 -> iconName = R.drawable.w11_light_snow;
            621 -> iconName = R.drawable.w11_snow;
            622 -> iconName = R.drawable.w11_snow_storm;
        }
    }
    // Rain
    if (weatherId in 500..531) {
        when (weatherId) {
            500 -> iconName = R.drawable.w11_rain;
            501 -> iconName = R.drawable.w11_rain;
            502 -> iconName = R.drawable.w11_heavy_rain;
            503 -> iconName = R.drawable.w11_very_heavy_rain;
            504 -> iconName = R.drawable.w11_extreme_rain;
            511 -> iconName = R.drawable.w11_sleet;
            520 -> iconName = R.drawable.w11_rain;
            521 -> iconName = R.drawable.w11_rain;
            522 -> iconName = R.drawable.w11_heavy_rain;
            531 -> iconName = R.drawable.w11_heavy_rain;
        }
    }
    // Drizzle
    if (weatherId in 300..321) {
        when (weatherId) {
            300 -> iconName = R.drawable.w11_drizzle;
            301 -> iconName = R.drawable.w11_drizzle;
            302 -> iconName = R.drawable.w11_drizzle;
            310 -> iconName = R.drawable.w11_rain;
            311 -> iconName = R.drawable.w11_rain;
            312 -> iconName = R.drawable.w11_heavy_rain;
            313 -> iconName = R.drawable.w11_rain;
            314 -> iconName = R.drawable.w11_heavy_rain;
            321 -> iconName = R.drawable.w11_drizzle;
        }
    }
    // Thunderstorms
    if (weatherId in 200..232) {
        when (weatherId) {
            200 -> iconName = R.drawable.w11_storm;
            201 -> iconName = R.drawable.w11_storm;
            202 -> iconName = R.drawable.w11_heavy_storm;
            210 -> iconName = R.drawable.w11_storm;
            211 -> iconName = R.drawable.w11_storm;
            212 -> iconName = R.drawable.w11_heavy_storm;
            221 -> iconName = R.drawable.w11_rain;
            230 -> iconName = R.drawable.w11_storm;
            231 -> iconName = R.drawable.w11_storm;
            232 -> iconName = R.drawable.w11_heavy_storm;
        }
    }

    return iconName;
}

fun getBatteryIcon(batteryPercentage: Int): Int {
    var batteryIcon = R.drawable.batterycharging20

    if (batteryPercentage <= 20) {
        batteryIcon = R.drawable.batterycharging20
    } else if (batteryPercentage <= 30) {
        batteryIcon = R.drawable.batterycharging30
    } else if (batteryPercentage <= 50) {
        batteryIcon = R.drawable.batterycharging50
    } else if (batteryPercentage <= 60) {
        batteryIcon = R.drawable.batterycharging60
    } else if (batteryPercentage <= 80) {
        batteryIcon = R.drawable.batterycharging80
    } else if (batteryPercentage < 100) {
        batteryIcon = R.drawable.batterycharging90
    }

    return batteryIcon;
}