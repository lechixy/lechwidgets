package com.lechixy.lechwidgets.database

data class BoardState(
    val boards: List<Board> = emptyList(),
    val id: Int = 0,
    val user: String = "",
    val board: String = "",
    val frequency: String = "",
    val isAddingBoard: Boolean = false
)
