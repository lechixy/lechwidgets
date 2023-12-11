package com.lechixy.lechwidgets.widgets.glance

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chibatching.kotpref.Kotpref
import com.lechixy.lechsaver.components.LechDialog
import com.lechixy.lechwidgets.R
import com.lechixy.lechwidgets.common.GlanceUtil
import com.lechixy.lechwidgets.common.Preferences
import com.lechixy.lechwidgets.components.PreferenceItem
import com.lechixy.lechwidgets.components.PreferenceItemGroup
import com.lechixy.lechwidgets.services.BatteryReceiver
import com.lechixy.lechwidgets.services.NotificationListener
import com.lechixy.lechwidgets.ui.theme.LechWidgetsTheme


class GlanceActivity : ComponentActivity() {

    var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(
            R.anim.slide_in,
            androidx.appcompat.R.anim.abc_fade_out
        );

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

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CALENDAR),
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

        // Preferences
        Kotpref.init(this.applicationContext)

        setContent {
            var colorfulMusicIconsState by remember { mutableStateOf(Preferences.colorfulMusicIcons) }
            var colorfulNotificationIconsState by remember { mutableStateOf(Preferences.colorfulNotificationIcons) }

            var showMusicIconShapeDialog by remember {
                mutableStateOf(false)
            }
            var showNotificationIconShapeDialog by remember {
                mutableStateOf(false)
            }
            var showNotificationFilterDialog by remember {
                mutableStateOf(false)
            }

            var musicIconShape by remember { mutableStateOf(Preferences.musicIconShape) }
            var notificationIconShape by remember { mutableStateOf(Preferences.notificationIconShape) }

            LechWidgetsTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { it ->

                    if (showMusicIconShapeDialog) {
                        LechDialog(
                            onDismissRequest = {
                                showMusicIconShapeDialog = false
                            },
                            title = { Text("Music icon shape") },
                            description = {
                                Text(
                                    "How music icon should look like?",
                                    textAlign = TextAlign.Center
                                )
                            },
                            icon = { Icon(painterResource(R.drawable.m3_shapes), null) },
                            text = {
                                Column {
                                    GlanceUtil.IconShapes.forEachIndexed { index, content ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp, 0.dp)
                                                .clickable {
                                                    musicIconShape =
                                                        GlanceUtil.IconShapes[index]
                                                    Preferences.musicIconShape =
                                                        GlanceUtil.IconShapes[index]
                                                    showMusicIconShapeDialog = false
                                                }
                                        ) {
                                            RadioButton(
                                                selected = musicIconShape == content,
                                                onClick = {}
                                            )
                                            Text(
                                                color = (
                                                        if (musicIconShape == content) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                        ),
                                                text = content,
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                    if (showNotificationFilterDialog) {
                        LechDialog(
                            onDismissRequest = {
                                showNotificationFilterDialog = false
                            },
                            title = { Text("Notification filter") },
                            description = {
                                Text(
                                    "Which notification categories you want to get?",
                                    textAlign = TextAlign.Center
                                )
                            },
                            icon = {
                                Icon(
                                    painterResource(R.drawable.m3_edit_notifications),
                                    null
                                )
                            },
                            text = {
                                Column {
                                    FlowRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(6.dp, 0.dp),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            4.dp,
                                            Alignment.CenterHorizontally
                                        ),
                                    ) {
                                        GlanceUtil.notificationCategories.forEach {
                                            var isSelected by remember {
                                                mutableStateOf(
                                                    Preferences.allowedNotificationCategories.contains(
                                                        it
                                                    )
                                                )
                                            }
                                            val text = it.split("_").joinToString(" ")
                                            var label = "${text.first().uppercase()}${text.drop(1)}"
                                            when (label) {
                                                "Msg" -> label = "Message"
                                                "Err" -> label = "Error"
                                                "Sys" -> label = "System"
                                            }

                                            FilterChip(
                                                selected = isSelected,
                                                leadingIcon = {
                                                    if (isSelected) {
                                                        Icon(Icons.Rounded.Check, null)
                                                    }
                                                },
                                                onClick = {
                                                    if (isSelected) {
                                                        Preferences.allowedNotificationCategories =
                                                            Preferences.allowedNotificationCategories.replace(
                                                                ",$it",
                                                                ""
                                                            )
                                                        isSelected = false
                                                    } else {
                                                        Preferences.allowedNotificationCategories =
                                                            "${Preferences.allowedNotificationCategories},$it"
                                                        isSelected = true
                                                    }
                                                },
                                                label = { Text(text = label) })
                                        }
                                    }
                                }
                            }
                        )
                    }
                    if (showNotificationIconShapeDialog) {
                        LechDialog(
                            onDismissRequest = {
                                showNotificationIconShapeDialog = false
                            },
                            title = { Text("Notification icon shape") },
                            description = {
                                Text(
                                    "How notification icon should look like?",
                                    textAlign = TextAlign.Center
                                )
                            },
                            icon = { Icon(painterResource(R.drawable.m3_shapes), null) },
                            text = {
                                Column {
                                    GlanceUtil.IconShapes.forEachIndexed { index, content ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp, 0.dp)
                                                .clickable {
                                                    notificationIconShape =
                                                        GlanceUtil.IconShapes[index]
                                                    Preferences.notificationIconShape =
                                                        GlanceUtil.IconShapes[index]
                                                    showNotificationIconShapeDialog = false
                                                }
                                        ) {
                                            RadioButton(
                                                selected = notificationIconShape == content,
                                                onClick = {}
                                            )
                                            Text(
                                                color = (
                                                        if (notificationIconShape == content) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                        ),
                                                text = content,
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
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
                        Spacer(
                            Modifier.height(20.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Music",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                            PreferenceItemGroup {
                                PreferenceItem(
                                    title = "Music icon shape",
                                    //description = "Choose which shape should icon shape look like",
                                    //descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    description = Preferences.musicIconShape,
                                    descriptionColor = MaterialTheme.colorScheme.primary,
                                    icon = painterResource(R.drawable.m3_shapes),
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    trailingIcon = {
                                        Icon(
                                            Icons.Outlined.Settings, null,
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    },
                                    trailingIconDivider = true,
                                    onClick = {
                                        showMusicIconShapeDialog = true
                                    }
                                )
                                PreferenceItem(
                                    title = "Colorful music icons",
                                    description = "Music players icons can be colorful (player icons have their theme color)",
                                    descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    icon = painterResource(R.drawable.m3_music),
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    trailingIcon = {
                                        Switch(
                                            checked = colorfulMusicIconsState,
                                            onCheckedChange = { switch ->
                                                colorfulMusicIconsState = switch
                                                Preferences.colorfulMusicIcons = switch
                                            },
                                        )
                                    },
                                    trailingIconDivider = false,
                                    onClick = {
                                        colorfulMusicIconsState = colorfulMusicIconsState.not()
                                        Preferences.colorfulMusicIcons = colorfulMusicIconsState
                                    }
                                )
                            }
                            Text(
                                text = "Notification",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                            PreferenceItemGroup {
                                PreferenceItem(
                                    title = "Notification filter",
                                    description = "Whether to show notifications by category",
                                    descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    icon = painterResource(R.drawable.m3_edit_notifications),
                                    iconColor = MaterialTheme.colorScheme.primary,
//                                trailingIcon = {
//
//                                },
//                                trailingIconDivider = false,
                                    onClick = {
                                        showNotificationFilterDialog = true
                                    }
                                )
                                PreferenceItem(
                                    title = "Notification icon shape",
                                    //description = "Choose which shape should icon shape look like",
                                    //descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    description = Preferences.notificationIconShape,
                                    descriptionColor = MaterialTheme.colorScheme.primary,
                                    icon = painterResource(R.drawable.m3_shapes),
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    trailingIcon = {
                                        Icon(
                                            Icons.Outlined.Settings, null,
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    },
                                    trailingIconDivider = true,
                                    onClick = {
                                        showNotificationIconShapeDialog = true
                                    }
                                )
                                PreferenceItem(
                                    title = "Colorful notification icons",
                                    description = "Notification icons can be color with their own color",
                                    descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    icon = Icons.Default.Notifications,
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    trailingIcon = {
                                        Switch(
                                            checked = colorfulNotificationIconsState,
                                            onCheckedChange = { switch ->
                                                colorfulNotificationIconsState = switch
                                                Preferences.colorfulNotificationIcons = switch
                                            },
                                        )
                                    },
                                    trailingIconDivider = false,
                                    onClick = {
                                        colorfulNotificationIconsState =
                                            colorfulNotificationIconsState.not()
                                        Preferences.colorfulNotificationIcons =
                                            colorfulNotificationIconsState
                                    }
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