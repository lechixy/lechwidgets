package com.lechixy.lechwidgets

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lechixy.lechwidgets.ui.theme.LechWidgetsTheme


class GlanceActivity : ComponentActivity() {

    var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Find the widget id from the intent.
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val appWidgetIds = AppWidgetManager.getInstance(application).getAppWidgetIds(
            ComponentName(
                application,
                LechGlance::class.java
            )
        )

        // Check permissions for need
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                0
            );
        }

        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )

        if (enabledListeners != null && enabledListeners.contains(packageName)) {
            val serviceIntent = Intent(this, NotificationListener::class.java)
            startService(serviceIntent)
        } else {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        if (appWidgetIds.isNotEmpty()) {
            val batteryReceiver = BatteryReceiver()
            application.registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        }

        setContent {
            val prefs = this.getSharedPreferences("LechGlance", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val toggleColorfulMusicIcons = prefs.getBoolean("toggleColorfulMusicIcons", true)

            var toggleColorfulMusicIconsState by remember { mutableStateOf(toggleColorfulMusicIcons) }

            LechWidgetsTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { it ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(onClick = {
                                val resultValue =
                                    Intent().putExtra(
                                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                                        appWidgetId
                                    )
                                setResult(RESULT_OK, resultValue)
                                finish()
                            }) {
                                Text(text = "Good to go")
                            }
                            Button(onClick = {
                                val appWidgetManager =
                                    AppWidgetManager.getInstance(this@GlanceActivity)
                                val myProvider =
                                    ComponentName(this@GlanceActivity, LechGlance::class.java)

                                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                    appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                }
                            }) {
                                Text(text = "Add widget")
                            }
                            Button(onClick = {
                                // Trigger widget update using an intent
                                // Trigger widget update using an intent
                                val intent = Intent(this@GlanceActivity, LechGlance::class.java)
                                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                                sendBroadcast(intent)
                                Toast.makeText(
                                    this@GlanceActivity,
                                    "Reloaded app widgets",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Text(text = "Reload widget")
                            }
                        }
                        Divider(
                            thickness = 20.dp,
                            color = MaterialTheme.colorScheme.background
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Music",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp, 10.dp)
                            ) {
                                Text(
                                    text = "Colorful music icons",
                                    fontSize = 18.sp
                                )
                                Switch(
                                    checked = toggleColorfulMusicIconsState,
                                    onCheckedChange = { switch ->
                                        toggleColorfulMusicIconsState = switch
                                        editor.putBoolean("toggleColorfulMusicIcons", switch)
                                        editor.apply()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(androidx.appcompat.R.anim.abc_fade_in, R.anim.slide_out)
    }
}