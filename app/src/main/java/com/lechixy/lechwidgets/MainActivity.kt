package com.lechixy.lechwidgets

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.lechixy.lechwidgets.ui.theme.LechWidgetsTheme


class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val prefs = this.getSharedPreferences("LechGlance", Context.MODE_PRIVATE)

            LechWidgetsTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { it ->
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Button(onClick = {
                                val intent = Intent(this@MainActivity, GlanceActivity::class.java)
                                startActivity(intent)
                                overridePendingTransition(
                                    R.anim.slide_in,
                                    androidx.appcompat.R.anim.abc_fade_out
                                );
                            }) {
                                Text(text = "Lech Glance")
                            }
                            Button(onClick = {

                            }) {
                                Text(text = "Lech Pinterest")
                            }
                        }
                    }
                }
            }
        }
    }
}