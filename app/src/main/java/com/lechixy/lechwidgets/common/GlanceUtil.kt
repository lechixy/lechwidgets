package com.lechixy.lechwidgets.common

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColor
import com.lechixy.lechwidgets.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone

class GlanceUtil {

    data class CalendarEvent(
        val title: String,
        val eventColor: Int,
        val isAllDay: Boolean,
        val startDate: Long,
        val endDate: Long
    )
    data class Weather(
        val iconName: Int,
        val weatherDescription: String
    )
    object IconShape{
        const val CIRCLE = "Circle"
        const val ROUNDED_CORNER= "Rounded Corner"
    }

    data class WidgetIcon(
        val iconResource: Int,
        val idAtGlance: Int
    )

    object GlanceTextType {
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
    }

    data class GlanceText(
        val textType: String,
        val idAtGlance: Int
    )

    data class PreferColor(
        val titleColor: Int,
        val subtitleColor: Int
    )

    object Events {
        const val UPDATE_MUSIC = "com.lechixy.lechwidgets.UPDATE_MUSIC"
        const val NEW_NOTIFICATION = "com.lechixy.lechwidgets.NEW_NOTIFICATION"
        const val REMOVE_NOTIFICATION = "com.lechixy.lechwidgets.REMOVE_NOTIFICATION"
        const val UPDATE_BATTERY = "com.lechixy.lechwidgets.UPDATE_BATTERY"
    }

    object Colors {
        val BATTERY_LOW = Color.rgb(225, 52, 55)
        val BATTERY_FULL = Color.rgb(61, 220, 133)
    }

    object ThemeColors {
        val LIGHT = PreferColor(
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#EBEBEB")
        )
        val DARK = PreferColor(
            Color.parseColor("#000000"),
            Color.parseColor("#141414")
        )
    }

    object GlanceThemeColors {
        val AUTO = "Auto"
        val LIGHT = "Light"
        val DARK = "Dark"
    }

    companion object {

        val GlanceTheme = listOf(
            "Auto",
            "Light",
            "Dark"
        )

        fun getPlayerIconColor(app: String): Int {
            var iconColor = Color.WHITE
            when(app){
                "com.spotify.music" -> {
                    iconColor = Color.rgb(30, 215, 96).toColor().toArgb()
                }
                "com.google.android.youtube" -> {
                    iconColor = Color.rgb(255, 0, 0).toColor().toArgb()
                }
                "com.google.android.apps.youtube.music" -> {
                    iconColor = Color.rgb(255, 0, 0).toColor().toArgb()
                }
                "com.apple.android.music" -> {
                    iconColor = Color.rgb(252, 60, 68).toColor().toArgb()
                }
            }
            return iconColor
        }
        
        fun getPreferColor(isWallpaperSupportsLight: Boolean): PreferColor{
            return if(isWallpaperSupportsLight){
                PreferColor(
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#EBEBEB")
                )
            } else {
                PreferColor(
                    Color.parseColor("#000000"),
                    Color.parseColor("#141414")
                )
            }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        fun isWallpaperSupportsLight(colorHint: Int): Boolean {
            return if (colorHint == 6){
                true
            } else if (colorHint == 4){
                false
            } else {
                false
            }
        }

        fun getAllIconsOnWidget(): List<WidgetIcon> {
            return listOf(
                WidgetIcon(R.drawable.m3_location, R.id.locationIcon)
            )
        }

        fun getAllTextsOnWidget(): List<GlanceText> {
            return listOf(
                GlanceText(GlanceTextType.TITLE, R.id.timeText), GlanceText(GlanceTextType.SUBTITLE, R.id.timeTextExtension),
                GlanceText(GlanceTextType.TITLE, R.id.date), GlanceText(GlanceTextType.TITLE, R.id.weatherTemp),
                /*GlanceText(GlanceTextType.TITLE, R.id.todaysEvent),*/ GlanceText(GlanceTextType.SUBTITLE, R.id.weatherDescription),
                GlanceText(GlanceTextType.SUBTITLE, R.id.location), GlanceText(GlanceTextType.TITLE, R.id.notification),
                GlanceText(GlanceTextType.TITLE, R.id.music), //GlanceText(GlanceTextType.SUBTITLE, R.id.battery),
            )
        }

        @SuppressLint("Range")
        fun getTodaysFirstEvent(context: Context): CalendarEvent? {
            val today = LocalDate.now()
                .atTime(LocalTime.of(0, 0))
                .toInstant(ZoneOffset.UTC).toEpochMilli()

            val after = Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
            }.timeInMillis

            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DISPLAY_COLOR,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            )

            //
            val selection = (
                    "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTEND} <= ?"
                    )
            val selectionArgs = arrayOf(
                today.toString(),
                after.toString() // Adding milliseconds for a day to get events till end of today
            )

            val uri = CalendarContract.Events.CONTENT_URI

            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )

            val events: MutableList<CalendarEvent?> = mutableListOf()
            cursor?.use {
                while (it.moveToNext()) {
                    val title = it.getString(it.getColumnIndex(CalendarContract.Events.TITLE))
                    val eventColor = it.getInt(it.getColumnIndex(CalendarContract.Events.DISPLAY_COLOR))
                    val isAllDay = it.getInt(it.getColumnIndex(CalendarContract.Events.ALL_DAY))
                    val startTime = it.getLong(it.getColumnIndex(CalendarContract.Events.DTSTART))
                    val endTime = it.getLong(it.getColumnIndex(CalendarContract.Events.DTEND))

                    events.add(
                        CalendarEvent(
                            title,
                            eventColor,
                            when(isAllDay){
                                1 -> true
                                else -> false
                            },
                            startTime,
                            endTime
                        )
                    )
                }
            }
            cursor?.close()

            return if(events.isEmpty()){
                null
            } else events.first()
        }

        val IconShapes = listOf(
            IconShape.CIRCLE,
            IconShape.ROUNDED_CORNER
        )

        fun getNotificationIconShape(notificationIconShapes: String): Int {
            return when(notificationIconShapes){
                IconShape.CIRCLE -> R.drawable.circle
                IconShape.ROUNDED_CORNER ->  R.drawable.round_rect
                else -> R.drawable.circle
            }
        }

        val notificationCategories: List<String> = listOf(
            NotificationCompat.CATEGORY_ALARM,
            NotificationCompat.CATEGORY_CALL,
            NotificationCompat.CATEGORY_EMAIL,
            NotificationCompat.CATEGORY_ERROR,
            NotificationCompat.CATEGORY_EVENT,
            NotificationCompat.CATEGORY_LOCATION_SHARING,
            NotificationCompat.CATEGORY_MESSAGE,
            NotificationCompat.CATEGORY_MISSED_CALL,
            NotificationCompat.CATEGORY_NAVIGATION,
            NotificationCompat.CATEGORY_PROGRESS,
            NotificationCompat.CATEGORY_PROMO,
            NotificationCompat.CATEGORY_RECOMMENDATION,
            NotificationCompat.CATEGORY_REMINDER,
            NotificationCompat.CATEGORY_SERVICE,
            NotificationCompat.CATEGORY_SOCIAL,
            NotificationCompat.CATEGORY_STATUS,
            NotificationCompat.CATEGORY_STOPWATCH,
            NotificationCompat.CATEGORY_SYSTEM,
            NotificationCompat.CATEGORY_TRANSPORT,
            NotificationCompat.CATEGORY_WORKOUT
        )
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

        fun getWeather(weatherId: Int, dayOrNight: String): Weather {
            var iconName: Int
            var weatherDescription: String

            // Clear Sky
            if (weatherId == 800 && dayOrNight == "day") {
                iconName = R.drawable.w11_sun
                weatherDescription = "Clear Sky"
            } else {
                iconName = R.drawable.w11_moon
                weatherDescription = "Clear Sky"
            }

            // Partly Cloudy
            if (weatherId in 801..804 && dayOrNight == "day") {
                when (weatherId) {
                    801 -> {
                        iconName = R.drawable.w11_day_partly_cloudy
                        weatherDescription = "Few Clouds"
                    }
                    802 -> {
                        iconName = R.drawable.w11_day_mostly_cloudy
                        weatherDescription = "Scattered Clouds"
                    };
                    803 -> {
                        iconName = R.drawable.w11_day_full_cloudy
                        weatherDescription = "Broken Clouds"
                    };
                    804 -> {
                        iconName = R.drawable.w11_day_full_cloudy
                        weatherDescription = "Overcast Clouds"
                    };
                }
            } else {
                when (weatherId) {
                    801 -> {
                        iconName = R.drawable.w11_night_partly_cloudy
                        weatherDescription = "Few Clouds"
                    }
                    802 -> {
                        iconName = R.drawable.w11_night_partly_cloudy
                        weatherDescription = "Scattered Clouds"
                    }
                    803 -> {
                        iconName = R.drawable.w11_night_mostly_cloudy
                        weatherDescription = "Broken Clouds"
                    }
                    804 -> {
                        iconName = R.drawable.w11_night_mostly_cloudy
                        weatherDescription = "Overcast Clouds"
                    }
                }
            }

            // Atmosphere
            if (weatherId in 701..781) {
                when (weatherId) {
                    701 -> {
                        iconName = R.drawable.w11_fog
                        weatherDescription = "Mist"
                    }
                    711 -> {
                        iconName = R.drawable.w11_fog
                        weatherDescription = "Smoke"
                    }
                    721 -> {
                        iconName = R.drawable.w11_haze
                        weatherDescription = "Haze"
                    }
                    731 -> {
                        iconName = R.drawable.w11_hurricane
                        weatherDescription = "Sand/Dust Whirls"
                    }
                    741 -> {
                        iconName = R.drawable.w11_fog
                        weatherDescription = "Fog"
                    }
                    751 -> {
                        iconName = R.drawable.w11_dust
                        weatherDescription = "Sand"
                    }
                    761 -> {
                        iconName = R.drawable.w11_dust
                        weatherDescription = "Dust"
                    }
                    762 -> {
                        iconName = R.drawable.w11_dust
                        weatherDescription = "Volcanic Ash"
                    }
                    771 -> {
                        iconName = R.drawable.w11_hurricane
                        weatherDescription = "Squalls"
                    }
                    781 -> {
                        iconName = R.drawable.w11_tornado
                        weatherDescription = "Tornado"
                    }
                }
            }
            // Snow
            if (weatherId in 600..622) {
                when (weatherId) {
                    600 -> {
                        iconName = R.drawable.w11_light_snow;
                        weatherDescription = "Light Snow"
                    }
                    601 -> {
                        iconName = R.drawable.w11_snow
                        weatherDescription = "Snow"
                    };
                    602 -> {
                        iconName = R.drawable.w11_snow_storm
                        weatherDescription = "Heavy Snow"
                    };
                    611 -> {
                        iconName = R.drawable.w11_sleet
                        weatherDescription = "Sleet"
                    };
                    612 -> {
                        iconName = R.drawable.w11_sleet
                        weatherDescription = "Light Sleet"
                    };
                    613 -> {
                        iconName = R.drawable.w11_sleet
                        weatherDescription = "Shower Sleet"
                    };
                    615 -> {
                        iconName = R.drawable.w11_sleet
                        weatherDescription = "Light Rain And Snow"
                    };
                    616 -> {
                        iconName = R.drawable.w11_sleet
                        weatherDescription = "Rain And Snow"
                    };
                    620 -> {
                        iconName = R.drawable.w11_light_snow
                        weatherDescription = "Light Shower Snow"
                    };
                    621 -> {
                        iconName = R.drawable.w11_snow
                        weatherDescription = "Shower Snow"
                    };
                    622 -> {
                        iconName = R.drawable.w11_snow_storm
                        weatherDescription = "Heavy Shower Snow"
                    };
                }
            }
            // Rain
            if (weatherId in 500..531) {
                when (weatherId) {
                    500 -> {
                        iconName = R.drawable.w11_rain
                        weatherDescription = "Light Rain"
                    };
                    501 -> {
                        iconName = R.drawable.w11_rain
                        weatherDescription = "Moderate Rain"
                    };
                    502 -> {
                        iconName = R.drawable.w11_heavy_rain
                        weatherDescription = "Heavy Rain"
                    };
                    503 -> {
                        iconName = R.drawable.w11_very_heavy_rain
                        weatherDescription = "Very Heavy Rain"
                    };
                    504 -> {
                        iconName = R.drawable.w11_extreme_rain
                        weatherDescription = "Extreme Rain"
                    };
                    511 -> {
                        iconName = R.drawable.w11_sleet
                        weatherDescription = "Freezing Rain"
                    };
                    520 -> {
                        iconName = R.drawable.w11_rain
                        weatherDescription = "Light Rain"
                    };
                    521 -> {
                        iconName = R.drawable.w11_rain
                        weatherDescription = "Shower Rain"
                    };
                    522 -> {
                        iconName = R.drawable.w11_heavy_rain
                        weatherDescription = "Heavy Shower Rain"
                    };
                    531 -> {
                        iconName = R.drawable.w11_heavy_rain
                        weatherDescription = "Ragged Shower Rain"
                    };
                }
            }
            // Drizzle
            if (weatherId in 300..321) {
                when (weatherId) {
                    300 -> {
                        iconName = R.drawable.w11_rain
                        weatherDescription = "Light Drizzle"
                    };
                    301 -> {
                        iconName = R.drawable.w11_rain
                        weatherDescription = "Drizzle"
                    };
                    302 -> {
                        iconName = R.drawable.w11_heavy_rain
                        weatherDescription = "Heavy Drizzle"
                    };
                    310 -> {
                        iconName = R.drawable.w11_rain
                        weatherDescription = "Light Drizzle Rain"
                    };
                    311 -> {
                        iconName = R.drawable.w11_rain
                        weatherDescription = "Drizzle Rain"
                    };
                    312 -> {
                        iconName = R.drawable.w11_heavy_rain
                        weatherDescription = "Heavy Drizzle Rain"
                    };
                    313 -> {
                        iconName = R.drawable.w11_rain
                        weatherDescription = "Shower Drizzle Rain"
                    };
                    314 -> {
                        iconName = R.drawable.w11_heavy_rain
                        weatherDescription = "Heavy Shower Drizzle Rain"
                    };
                    321 -> {
                        iconName = R.drawable.w11_drizzle
                        weatherDescription = "Shower Drizzle"
                    };
                }
            }
            // Thunderstorms
            if (weatherId in 200..232) {
                when (weatherId) {
                    200 -> {
                        iconName = R.drawable.w11_storm
                        weatherDescription = "Light Rain Thunderstorm"
                    };
                    201 -> {
                        iconName = R.drawable.w11_storm
                        weatherDescription = "Rain Thunderstorm"
                    };
                    202 -> {
                        iconName = R.drawable.w11_heavy_storm
                        weatherDescription = "Heavy Rain Thunderstorm"
                    };
                    210 -> {
                        iconName = R.drawable.w11_storm
                        weatherDescription = "Light Thunderstorm"
                    };
                    211 -> {
                        iconName = R.drawable.w11_storm
                        weatherDescription = "Thunderstorm"
                    };
                    212 -> {
                        iconName = R.drawable.w11_heavy_storm
                        weatherDescription = "Heavy Thunderstorm"
                    };
                    221 -> {
                        iconName = R.drawable.w11_heavy_storm
                        weatherDescription = "Ragged Thunderstorm"
                    };
                    230 -> {
                        iconName = R.drawable.w11_storm
                        weatherDescription = "Light Drizzle Thunderstorm"
                    };
                    231 -> {
                        iconName = R.drawable.w11_storm
                        weatherDescription = "Drizzle Thunderstorm"
                    };
                    232 -> {
                        iconName = R.drawable.w11_heavy_storm
                        weatherDescription = "Heavy Drizzle Thunderstorm"
                    };
                }
            }

            return Weather(
                iconName,
                weatherDescription
            )
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
    }
}

private fun Boolean.toInt() = if (this) 1 else 0
