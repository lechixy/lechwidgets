package com.lechixy.lechwidgets.page.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lechixy.lechwidgets.R
import com.lechixy.lechwidgets.common.Preferences
import com.lechixy.lechwidgets.components.BackButton
import com.lechixy.lechwidgets.components.ColoredAppText
import com.lechixy.lechwidgets.components.PreferenceItem
import com.lechixy.lechwidgets.components.PreferenceItemGroup
import com.lechixy.lechwidgets.widgets.bing.LechBing
import com.lechixy.lechwidgets.widgets.bing.LechGoogle
import com.lechixy.lechwidgets.widgets.glance.LechGlance
import com.lechixy.lechwidgets.widgets.pinterest.LechPinterest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun General(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true })

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.general)) },
                navigationIcon = {
                    BackButton { onBackPressed() }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            item {
                PreferenceItemGroup {
                    PreferenceItem(
                        title = "Reload all widgets",
                        description = "If you need to restart all widgets of app use this",
                        descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = Icons.Default.Refresh,
                        iconColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            // Glance
                            val glanceWidgets = AppWidgetManager.getInstance(context).getAppWidgetIds(
                                ComponentName(
                                    context,
                                    LechGlance::class.java
                                )
                            )
                            val glanceIntent = Intent(context, LechGlance::class.java).apply {
                                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, glanceWidgets)
                            }
                            context.sendBroadcast(glanceIntent)
                            // Pinterest
                            val pinterestWidgets = AppWidgetManager.getInstance(context).getAppWidgetIds(
                                ComponentName(
                                    context,
                                    LechPinterest::class.java
                                )
                            )
                            val pinterestIntent = Intent(context, LechPinterest::class.java).apply {
                                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, pinterestWidgets)
                            }
                            context.sendBroadcast(pinterestIntent)
                            // Bing
                            val bingWidgets = AppWidgetManager.getInstance(context).getAppWidgetIds(
                                ComponentName(
                                    context,
                                    LechBing::class.java
                                )
                            )
                            val bingIntent = Intent(context, LechBing::class.java).apply {
                                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, bingWidgets)
                            }
                            Preferences.todaysImage = ""
                            context.sendBroadcast(bingIntent)
                            // Google
                            val googleWidgets = AppWidgetManager.getInstance(context).getAppWidgetIds(
                                ComponentName(
                                    context,
                                    LechGoogle::class.java
                                )
                            )
                            val googleIntent = Intent(context, LechGoogle::class.java).apply {
                                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, googleWidgets)
                            }
                            context.sendBroadcast(googleIntent)

                            Toast.makeText(
                                context,
                                "Reloaded all widgets",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}