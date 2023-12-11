package com.lechixy.lechwidgets.database

sealed interface BoardEvent{
    object SaveBoard: BoardEvent
    data class SetId(val id: Int): BoardEvent
    data class SetUser(val user: String): BoardEvent
    data class SetBoard(val board: String): BoardEvent
    data class SetFrequency(val frequency: String): BoardEvent
    object ShowDialog: BoardEvent
    object HideDialog: BoardEvent
    data class DeleteBoard(val board: Board): BoardEvent
    data class GetBoardById(val id: Int): BoardEvent
}