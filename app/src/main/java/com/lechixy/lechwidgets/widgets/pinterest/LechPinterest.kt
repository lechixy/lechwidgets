package com.lechixy.lechwidgets.widgets.pinterest

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
import android.opengl.Visibility
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.lechixy.lechwidgets.database.Board
import com.lechixy.lechwidgets.database.BoardDatabase
import com.lechixy.lechwidgets.database.BoardPin
import com.lechixy.lechwidgets.R
import com.lechixy.lechwidgets.common.PinterestUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


class LechPinterest : AppWidgetProvider() {

    // Network
    var network: OkHttpClient = OkHttpClient()

    // Notification
    private val handler = Handler(Looper.getMainLooper())

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val views = RemoteViews(context.packageName, R.layout.lech_pinterest)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = getActiveWidgetIds(context)

        if (intent.action.equals(PinterestUtil.Events.UPDATE_PIN)) {
            Log.i("LECH", "YES IT WORKS")
//            val currentTime = LocalTime.now()
//            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
//            val formattedTime = currentTime.format(formatter)
//            views.setTextViewText(R.id.updateText, "Last update: ${formattedTime}")
//            appWidgetManager.updateAppWidget(appWidgetIds, views)

            val db = BoardDatabase.getInstance(context)
            val queue = Volley.newRequestQueue(context)
            val appWidgetId = intent.extras!!.getInt(PinterestUtil.UPDATE_PIN_WIDGET_ID)

            val board: Board?
            runBlocking(Dispatchers.IO) {
                board = db.boardDao.getId(appWidgetId).first()
            }

            if (board != null) {
                GlobalScope.launch(Dispatchers.IO) {
                    val boardContent =
                        db.boardContentDao.getBoardContentByKey("${board.user}/${board.board}")
                    if (boardContent != null) {
                        var currentPin: BoardPin = boardContent.contents.first()
                        if (board.pin.isNotBlank()) {
                            val matchedPin = boardContent.contents.indexOfFirst {
                                it.image == board.pin
                            }

                            // We put first condition because matchedPin equals to -1 means
                            // there is no matched pin in boardcontent contents
                            // so if we increment it will break the condition
                            if (
                                matchedPin != -1 &&
                                boardContent.contents.size > (matchedPin + 1)
                            ) {
                                currentPin = boardContent.contents[matchedPin + 1]
                            }
                        }

                        val imageRequest = ImageRequest(
                            currentPin.image,
                            { pinImage ->
//                                 TODO SAVING PINS TO A FOLDER

//                                    val filename = "${boardContent.contents.first().image
//                                        .replace(".jpg", "")
//                                        .split('/').last()}.png"
//                                    val fileUri = saveBitmap(
//                                        context,
//                                        pinImage,
//                                        Bitmap.CompressFormat.PNG,
//                                        "image/png",
//                                        filename
//                                    )
//                                    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//                                    downloadDir

                                val pinterestIntent = PendingIntent.getActivity(
                                    context,
                                    0,
                                    Intent(Intent.ACTION_VIEW, Uri.parse(currentPin.link)),
                                    PendingIntent.FLAG_IMMUTABLE
                                )
                                views.setOnClickPendingIntent(R.id.pinImage, pinterestIntent)
                                views.setImageViewBitmap(R.id.pinImage, pinImage)
                                appWidgetManager.updateAppWidget(appWidgetIds, views)
                            },
                            0, 0, ImageView.ScaleType.CENTER, Bitmap.Config.ARGB_8888,
                            {
                                Log.i("LECH", "Can't get the image")
                            }
                        )

                        db.boardDao.upsertBoard(
                            board.copy(
                                pin = currentPin.image
                            )
                        )

                        queue.add(imageRequest)
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        db: BoardDatabase
    ) {

        Log.i("LECH", "came here")

        createNotificationChannel(
            context,
            "pin_update",
            "Pin update notifications",
            "Notify you while updating pin widgets",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationBuilder = NotificationCompat.Builder(context, "pin_update")
            .setSmallIcon(R.drawable.m3_push_pin)
            .setContentTitle("Updating pin")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(Color.RED)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(1, notificationBuilder.build())
        }

        val board: Board?
        runBlocking(Dispatchers.IO) {
            board = db.boardDao.getId(appWidgetId).first()
        }

        if (board != null) {
            GlobalScope.launch(Dispatchers.IO) {
                val boardContent =
                    db.boardContentDao.getBoardContentByKey("${board.user}/${board.board}")
                if (boardContent != null) {
                    val formatter = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH)
                    val dateTime = LocalDateTime.parse(boardContent.date, formatter)
                    val parsedZonedDateTime = ZonedDateTime.of(dateTime, ZoneOffset.UTC)
                    val currentZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)

                    val sixHoursAgo = currentZonedDateTime.minusHours(6)

                    // If this board content not fetched in six hours then update it with new pins
                    if (parsedZonedDateTime.isAfter(sixHoursAgo).not()) {
                        val newContent = PinterestUtil.getRssChannelContent(board.user, board.board)
                        if(newContent != null){
                            db.boardContentDao.upsertBoardContent(newContent)
                        }
                    }

                    val updatePinIntent = Intent(PinterestUtil.Events.UPDATE_PIN)
                        .putExtra(PinterestUtil.UPDATE_PIN_WIDGET_ID, appWidgetId)

                    this@LechPinterest.onReceive(
                        context,
                        updatePinIntent
                    )

                    // Update Widget
                    val pinPendingIntent = getUpdatePinPendingIntent(context, appWidgetId)

                    val customFrequency = PinterestUtil.getFrequencyToLong(board.frequency)
                    val initialDelay = SystemClock.elapsedRealtime() + customFrequency

                    context.alarmManager.setInexactRepeating(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        initialDelay,
                        customFrequency,
                        pinPendingIntent
                    )
                }
            }
        } else {
            Log.i("LECH", "came to update but there is no board to update")
        }

        handler.postDelayed(Runnable {
            notificationManager.cancel(1)
        }, 1500)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val db = BoardDatabase.getInstance(context)

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, db)
        }
        scheduleWidgetUpdates(context, db)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context);
        val db = BoardDatabase.getInstance(context)
        // Enter relevant functionality for when the first widget is created
        scheduleWidgetUpdates(context, db)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds);
        val db = BoardDatabase.getInstance(context)

        // TODO db.boardDao.deleteBoard()
        //appWidgetIds.forEach {  }

        // reschedule update alarm so it does not include ID of currently removed widget
        scheduleWidgetUpdates(context, db)
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

    private fun scheduleWidgetUpdates(context: Context, db: BoardDatabase) {
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

    private fun getUpdatePinPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val widgetClass = this::class.java

        val updateIntent = Intent(context, widgetClass)
            .setAction(PinterestUtil.Events.UPDATE_PIN)
            .putExtra(PinterestUtil.UPDATE_PIN_WIDGET_ID, appWidgetId)

        val requestCode = widgetClass.name.hashCode()
        val flags = PendingIntent.FLAG_CANCEL_CURRENT or
                PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getBroadcast(context, requestCode, updateIntent, flags)
    }


    private val Context.alarmManager: AlarmManager
        get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun createNotificationChannel(context: Context, id: String, name: String,  description: String, importance: Int) {
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

//    @Throws(IOException::class)
//    fun saveBitmap(
//        context: Context, bitmap: Bitmap, format: Bitmap.CompressFormat,
//        mimeType: String, displayName: String
//    ): android.net.Uri {
//
//        val values = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
//            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
//            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
//        }
//
//        var uri: android.net.Uri? = null
//
//        return runCatching {
//            with(context.contentResolver) {
//                insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.also {
//                    uri = it // Keep uri reference so it can be removed on failure
//
//                    openOutputStream(it)?.use { stream ->
//                        if (!bitmap.compress(format, 100, stream))
//                            throw IOException("Failed to save bitmap.")
//                    } ?: throw IOException("Failed to open output stream.")
//
//                } ?: throw IOException("Failed to create new MediaStore record.")
//            }
//        }.getOrElse {
//            uri?.let { orphanUri ->
//                // Don't leave an orphan entry in the MediaStore
//                context.contentResolver.delete(orphanUri, null, null)
//            }
//
//            throw it
//        }
//    }
}