package com.lechixy.lechwidgets.widgets.pinterest

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.lechixy.lechwidgets.components.LechDialog
import com.lechixy.lechwidgets.database.BoardDatabase
import com.lechixy.lechwidgets.database.BoardEvent
import com.lechixy.lechwidgets.database.BoardViewModel
import com.lechixy.lechwidgets.R
import com.lechixy.lechwidgets.common.PinterestUtil
import com.lechixy.lechwidgets.components.ConfirmButton
import com.lechixy.lechwidgets.components.PreferenceItem
import com.lechixy.lechwidgets.components.PreferenceItemGroup
import com.lechixy.lechwidgets.ui.theme.LechWidgetsTheme
import com.lechixy.lechwidgets.widgets.glance.LechGlance
import com.prof18.rssparser.RssParserBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PinterestActivity : ComponentActivity() {

    var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            BoardDatabase::class.java,
            "boards.db"
        ).build()
    }
    private val viewModel by viewModels<BoardViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BoardViewModel(db.boardDao) as T
                }
            }
        }
    )

    @OptIn(ExperimentalMaterial3Api::class)
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
        Log.i("LECH", appWidgetId.toString())

        val appWidgetIds = AppWidgetManager.getInstance(application).getAppWidgetIds(
            ComponentName(
                application,
                PinterestActivity::class.java
            )
        )

        setContent {
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            val state by viewModel.state.collectAsState()
            val onEvent = viewModel::onEvent

            onEvent(BoardEvent.SetId(appWidgetId))
            onEvent(BoardEvent.GetBoardById(appWidgetId))

            val builder = RssParserBuilder()
            val rssParser = builder.build()

            LechWidgetsTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState
                        )
                    },
                    floatingActionButton = {
                        if (appWidgetId != 0) {
                            ExtendedFloatingActionButton(
                                text = { Text("Good to go") },
                                icon = { Icon(painterResource(R.drawable.m3_done_all), null) },
                                onClick = {
                                    if (state.user.isNotEmpty() && state.board.isNotEmpty() && state.frequency.isNotEmpty()) {
                                        scope.launch(Dispatchers.IO) {
                                            val result = PinterestUtil.getRssChannelContent(state.user, state.board)
                                            if (result != null) {
                                                db.boardContentDao.upsertBoardContent(result)
                                                onEvent(BoardEvent.SaveBoard)

                                                val intent =
                                                    Intent(
                                                        this@PinterestActivity,
                                                        LechPinterest::class.java
                                                    )
                                                intent.action =
                                                    AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                                intent.putExtra(
                                                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                                                    intArrayOf(appWidgetId)
                                                )
                                                sendBroadcast(intent)

                                                val resultValue =
                                                    Intent().putExtra(
                                                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                                                        appWidgetId
                                                    )
                                                setResult(RESULT_OK, resultValue)
                                                finish()
                                            } else {
                                                snackbarHostState.showSnackbar(
                                                    "There is no user or board like this please check it",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Fill all the fields to create widget",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                })
                        }
                    },
                    topBar = {
                        if (appWidgetId != 0) {
                            CenterAlignedTopAppBar(
                                title = { Text(
                                    "Editing widget",
                                ) },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                ) { it ->
                    // If this is a widget configuration
                    if (appWidgetId != 0) {
                        var showUserDialog by remember { mutableStateOf(false) }
                        var showBoardDialog by remember { mutableStateOf(false) }
                        var showFrequencyDialog by remember { mutableStateOf(false) }

                        val userDescription = state.user.ifEmpty {
                            "Set User"
                        }
                        val boardDescription = state.board.ifEmpty {
                            "Set Board"
                        }
                        val frequencyDescription = state.frequency.ifEmpty {
                            "Set Frequency"
                        }

                        if (showUserDialog) {
                            LechDialog(
                                onDismissRequest = {
                                    showUserDialog = false
                                },
                                title = { Text("User of board") },
                                description = { Text("Who is the owner of this board?") },
                                icon = { Icon(Icons.Default.AccountCircle, null) },
                                text = {
                                    Column(
                                        modifier = Modifier.padding(14.dp, 0.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = state.user,
                                            label = { Text("Username") },
                                            onValueChange = {
                                                onEvent(BoardEvent.SetUser(it))
                                            },
                                        )
                                    }
                                },
                                confirmButton = {
                                    ConfirmButton {
                                        onEvent(BoardEvent.SaveBoard)
                                        showUserDialog = false
                                    }
                                }
                            )
                        }
                        if (showBoardDialog) {
                            LechDialog(
                                onDismissRequest = {
                                    showBoardDialog = false
                                },
                                title = { Text("Name of board") },
                                description = { Text("What is the name of the board?") },
                                icon = { Icon(Icons.Default.Menu, null) },
                                text = {
                                    Column(
                                        modifier = Modifier.padding(14.dp, 0.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = state.board,
                                            label = { Text("Board name") },
                                            onValueChange = {
                                                onEvent(BoardEvent.SetBoard(it))
                                            },
                                        )
                                    }
                                },
                                confirmButton = {
                                    ConfirmButton {
                                        onEvent(BoardEvent.SaveBoard)
                                        showBoardDialog = false
                                    }
                                }
                            )
                        }
                        if (showFrequencyDialog) {
                            LechDialog(
                                onDismissRequest = {
                                    showFrequencyDialog = false
                                },
                                title = { Text("Refresh Frequency") },
                                description = { Text("How often should we update this?") },
                                icon = { Icon(Icons.Default.Refresh, null) },
                                text = {
                                    Column {
                                        PinterestUtil.frequencyChoices.forEachIndexed { index, content ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp, 0.dp)
                                                    .clickable {
                                                        onEvent(BoardEvent.SetFrequency(content))
                                                        showFrequencyDialog = false
                                                    }
                                            ) {
                                                RadioButton(
                                                    selected = state.frequency == content,
                                                    onClick = {}
                                                )
                                                Text(
                                                    color = (
                                                            if (state.frequency == content) {
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {


//                            Text(
//                                fillValuesText,
//                                style = MaterialTheme.typography.titleSmall,
//                                color = MaterialTheme.colorScheme.onSurface
//                            )

//                            Spacer(modifier = Modifier.height(20.dp))

                            PreferenceItem(
                                title = "User name",
                                description = userDescription,
                                icon = Icons.Default.AccountCircle,
                                onClick = {
                                    showUserDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            PreferenceItem(
                                title = "Board name",
                                description = boardDescription,
                                icon = Icons.Default.Menu,
                                onClick = {
                                    showBoardDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            PreferenceItem(
                                title = "Refresh frequency of widget",
                                description = frequencyDescription,
                                icon = Icons.Default.Refresh,
                                onClick = {
                                    showFrequencyDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    val intent =
                                        Intent(this@PinterestActivity, LechPinterest::class.java)
                                    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                    intent.putExtra(
                                        AppWidgetManager.EXTRA_APPWIDGET_IDS,
                                        intArrayOf(appWidgetId)
                                    )
                                    sendBroadcast(intent)
                                    scope.launch(Dispatchers.Main){
                                        snackbarHostState.showSnackbar(
                                            "Pin is updated with the next one!",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            ) {
                                Text(text = "Next pin")
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(this@PinterestActivity, LechPinterest::class.java).apply {
                                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                        putExtra(
                                            AppWidgetManager.EXTRA_APPWIDGET_IDS,
                                            intArrayOf(appWidgetId)
                                        )
                                    }
                                    sendBroadcast(intent)
                                    scope.launch(Dispatchers.Main){
                                        snackbarHostState.showSnackbar(
                                            "Reloaded this widget",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            ) {
                                Text(text = "Reload this widget")
                            }
                        }
                    } else {
                        Column(
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
//                                        PreferenceItem(
//                                            title = "Glance Theme",
//                                            description = glanceTheme,
//                                            descriptionColor = MaterialTheme.colorScheme.primary,
//                                            icon = painterResource(R.drawable.m3_palette),
//                                            iconColor = MaterialTheme.colorScheme.primary,
//                                            trailingIcon = {
//                                                Icon(
//                                                    Icons.Outlined.Settings, null,
//                                                    tint = MaterialTheme.colorScheme.outline
//                                                )
//                                            },
//                                            trailingIconDivider = true,
//                                            onClick = {
//                                                showGlanceThemeDialog = true
//                                            }
//                                        )
                                        PreferenceItem(
                                            title = "Add widget",
                                            description = "You can add a delicious widget",
                                            descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            icon = Icons.Default.Add,
                                            iconColor = MaterialTheme.colorScheme.primary,
                                            onClick = {
                                                val appWidgetManager =
                                                    AppWidgetManager.getInstance(this@PinterestActivity)
                                                val myProvider =
                                                    ComponentName(
                                                        this@PinterestActivity,
                                                        LechPinterest::class.java
                                                    )
                                                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                                    appWidgetManager.requestPinAppWidget(
                                                        myProvider,
                                                        null,
                                                        null
                                                    )
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
                                                    this@PinterestActivity,
                                                    "Reloaded app widgets",
                                                    Toast.LENGTH_SHORT
                                                ).show()
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
    }

    private fun updateWidgets(appWidgetIds: IntArray) {
        val intent = Intent(this@PinterestActivity, LechPinterest::class.java).apply {
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