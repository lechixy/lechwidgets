package com.lechixy.lechwidgets.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lechixy.lechwidgets.R
import com.lechixy.lechwidgets.common.Route
import com.lechixy.lechwidgets.components.BackButton
import com.lechixy.lechwidgets.components.SettingItem
import com.lechixy.lechwidgets.ui.theme.LechWidgetsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navController: NavController,
    onBackPressed: () -> Unit
) {

//            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
//                rememberTopAppBarState(),
//                canScroll = { true })

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        // .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { BackButton { onBackPressed() } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
                //scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(it)
        ) {
            Text(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )

            SettingItem(
                title = stringResource(id = R.string.general),
                description = stringResource(
                    id = R.string.general_desc
                ),
                icon = Icons.Filled.Settings,
                onClick = {
                    navController.navigate(Route.SETTINGS_GENERAL)
                }
            )

            SettingItem(
                title = stringResource(id = R.string.about),
                description = stringResource(
                    id = R.string.about_desc
                ),
                icon = Icons.Filled.Info,
                onClick = {
                    navController.navigate(Route.SETTINGS_ABOUT)
                }
            )
        }
    }
}