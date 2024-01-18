package com.lechixy.lechwidgets.widgets.google

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chibatching.kotpref.Kotpref
import com.lechixy.lechwidgets.R
import com.lechixy.lechwidgets.common.GlanceUtil
import com.lechixy.lechwidgets.common.Preferences
import com.lechixy.lechwidgets.components.LechDialog
import com.lechixy.lechwidgets.components.PreferenceItem
import com.lechixy.lechwidgets.components.PreferenceItemGroup
import com.lechixy.lechwidgets.ui.theme.LechWidgetsTheme
import com.lechixy.lechwidgets.widgets.bing.LechGoogle
import com.lechixy.lechwidgets.widgets.glance.LechGlance

class GoogleActivity : ComponentActivity() {

    var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(
            androidx.appcompat.R.anim.abc_fade_in,
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
                LechGoogle::class.java
            )
        )

        // Preferences
        Kotpref.init(this.applicationContext)

        setContent {
            // General
            var showWidgetThemeDialog by remember { mutableStateOf(false) }
            var widgetTheme by remember { mutableStateOf(Preferences.googleWidgetTheme) }

            var wallpaperColorsApplyIcons by remember { mutableStateOf(Preferences.wallpaperColorsApplyIcons) }

            LechWidgetsTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        if (appWidgetId != 0) {
                            ExtendedFloatingActionButton(
                                text = { Text("Good to go") },
                                icon = { Icon(painterResource(R.drawable.m3_done_all), null) },
                                onClick = {
                                    val resultValue =
                                        Intent().putExtra(
                                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                                            appWidgetId
                                        )
                                    setResult(RESULT_OK, resultValue)
                                    finish()
                                }
                            )
                        }
                    },
                ) { it ->

                    if (showWidgetThemeDialog){
                        LechDialog(
                            onDismissRequest = {
                                showWidgetThemeDialog = false
                            },
                            title = { Text("Widget Theme") },
                            description = {
                                Text(
                                    "How widget should look like?",
                                    textAlign = TextAlign.Center
                                )
                            },
                            icon = { Icon(painterResource(R.drawable.m3_palette), null) },
                            text = {
                                Column {
                                    GlanceUtil.GlanceTheme.forEachIndexed { index, content ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp, 0.dp)
                                                .clickable {
                                                    widgetTheme = content
                                                    Preferences.googleWidgetTheme = content
                                                    showWidgetThemeDialog = false
                                                    updateWidgets(appWidgetIds)
                                                }
                                        ) {
                                            RadioButton(
                                                selected = widgetTheme == content,
                                                onClick = {}
                                            )
                                            Text(
                                                color = (
                                                        if (widgetTheme == content) {
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
                        LazyColumn(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth(),
                            userScrollEnabled = true,
                        ) {
                            item {
                                Text(
                                    text = "General",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(10.dp)
                                )
                                PreferenceItemGroup {
                                    PreferenceItem(
                                        title = "Widget Theme",
                                        description = widgetTheme,
                                        descriptionColor = MaterialTheme.colorScheme.primary,
                                        icon = painterResource(R.drawable.m3_palette),
                                        iconColor = MaterialTheme.colorScheme.primary,
                                        trailingIcon = {
                                            Icon(
                                                Icons.Outlined.Settings, null,
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        },
                                        trailingIconDivider = true,
                                        onClick = {
                                            showWidgetThemeDialog = true
                                        }
                                    )
                                    PreferenceItem(
                                        title = "Add widget",
                                        description = "You can add a delicious widget",
                                        descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        icon = Icons.Default.Add,
                                        iconColor = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            val appWidgetManager =
                                                AppWidgetManager.getInstance(this@GoogleActivity)
                                            val myProvider =
                                                ComponentName(this@GoogleActivity, LechGoogle::class.java)
                                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                                appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                            }
                                        }
                                    )
                                    PreferenceItem(
                                        title = "Reload widgets",
                                        description = "Sometimes the widget may freeze, reloading can solves the problem",
                                        descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        icon = Icons.Default.Refresh,
                                        iconColor = MaterialTheme.colorScheme.error,
                                        onClick = {
                                            updateWidgets(appWidgetIds)
                                            Toast.makeText(
                                                this@GoogleActivity,
                                                "Reloaded app widgets",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                                Text(
                                    text = "Customization",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(10.dp)
                                )
                                PreferenceItemGroup {
                                    PreferenceItem(
                                        title = "Apply wallpaper colors to icons",
                                        description = "Icons on the widget should have wallpapers colors",
                                        descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        icon = painterResource(R.drawable.m3_format_paint),
                                        iconColor = MaterialTheme.colorScheme.primary,
                                        trailingIcon = {
                                            Switch(
                                                checked = wallpaperColorsApplyIcons,
                                                onCheckedChange = { switch ->
                                                    wallpaperColorsApplyIcons = switch
                                                    Preferences.wallpaperColorsApplyIcons = switch
                                                    updateWidgets(appWidgetIds)
                                                },
                                            )
                                        },
                                        trailingIconDivider = false,
                                        onClick = {
                                            wallpaperColorsApplyIcons = wallpaperColorsApplyIcons.not()
                                            Preferences.wallpaperColorsApplyIcons = wallpaperColorsApplyIcons
                                            updateWidgets(appWidgetIds)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private fun updateWidgets(appWidgetIds: IntArray) {
        val intent = Intent(this@GoogleActivity, LechGoogle::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        sendBroadcast(intent)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(
            androidx.appcompat.R.anim.abc_fade_in,
            androidx.appcompat.R.anim.abc_fade_out
        );
    }
}