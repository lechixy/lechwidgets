package com.lechixy.lechwidgets.page

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.lechixy.lechwidgets.widgets.glance.GlanceActivity
import com.lechixy.lechwidgets.R
import com.lechixy.lechwidgets.components.ColoredAppText
import com.lechixy.lechwidgets.components.WidgetItem
import com.lechixy.lechwidgets.widgets.bing.BingActivity
import com.lechixy.lechwidgets.widgets.google.GoogleActivity
import com.lechixy.lechwidgets.widgets.pinterest.PinterestActivity


@SuppressLint("InlinedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    navigateToSettings: () -> Unit = {},
    activity: Activity,
) {
    val context = LocalContext.current

    if (ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            0
        );
    }


    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    ColoredAppText()
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navigateToSettings() }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        }
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(20.dp, 0.dp)
        ) {
            item {
                WidgetItem(
                    onClick = {
                        val intent = Intent(context, GoogleActivity::class.java)
                        context.startActivity(intent)
                    },
                    layout = R.layout.lech_google,
                    widgetName = stringResource(R.string.lech_google_name),
                    widgetDescription = stringResource(R.string.lech_google_description)
                )
                Spacer(
                    modifier = Modifier.height(20.dp)
                )
                WidgetItem(
                    onClick = {
                        val intent = Intent(context, BingActivity::class.java)
                        context.startActivity(intent)
                    },
                    layout = R.layout.lech_bing,
                    widgetName = stringResource(R.string.lech_bing_name),
                    widgetDescription = stringResource(R.string.lech_pinterest_description)
                )
                Spacer(
                    modifier = Modifier.height(20.dp)
                )
                WidgetItem(
                    onClick = {
                        val intent = Intent(context, GlanceActivity::class.java)
                        context.startActivity(intent)
                    },
                    layout = R.layout.lech_glance,
                    widgetName = stringResource(R.string.lech_glance_name),
                    widgetDescription = stringResource(R.string.lech_glance_description)
                )
                Spacer(
                    modifier = Modifier.height(20.dp)
                )
                WidgetItem(
                    onClick = {
                        val intent = Intent(context, PinterestActivity::class.java)
                        context.startActivity(intent)
                    },
                    layout = R.layout.lech_pinterest,
                    widgetName = stringResource(R.string.lech_pinterest_name),
                    widgetDescription = stringResource(R.string.lech_pinterest_description)
                )
            }
        }
    }
}