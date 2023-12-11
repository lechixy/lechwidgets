package com.lechixy.lechwidgets.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Board(
    val user: String,
    val board: String,
    val frequency: String,
    val pin: String,

    @PrimaryKey(autoGenerate = false)
    val id: Int = 0,
)

data class BoardPin(
    val title: String,
    val link: String,
    val description: String,
    val date: String,
    val image: String,
)

@Entity
data class BoardContent(
    val contents: List<BoardPin>,
    val date: String,
    val link: String,
    val title: String,

    @PrimaryKey(autoGenerate = false)
    val key: String,
)