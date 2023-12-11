package com.lechixy.lechwidgets.database

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BoardViewModel(
    private val dao: BoardDao
) : ViewModel() {

    private val _state = MutableStateFlow(BoardState())
    private val _boards = dao
        .getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val state = combine(_state, _boards) { state, boards ->
        state.copy(
            boards = boards
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BoardState())

    fun onEvent(event: BoardEvent) {
        when (event) {
            is BoardEvent.DeleteBoard -> {
                viewModelScope.launch {
                    dao.deleteBoard(event.board)
                }
            }

            BoardEvent.HideDialog -> {
                _state.update {
                    it.copy(
                        isAddingBoard = false
                    )
                }
            }

            BoardEvent.SaveBoard -> {
                val id = state.value.id
                val user = state.value.user
                val board = state.value.board
                val frequency = state.value.frequency

                if(user.isBlank() || board.isBlank() || frequency.isBlank()){
                    return
                }

                val boardObject = Board(
                    user,
                    board,
                    frequency,
                    "",
                    id
                )

                viewModelScope.launch {
                    dao.upsertBoard(boardObject)
                }
                _state.update { it.copy(
                    isAddingBoard = false,
                    id = 0,
                    user = "",
                    board = "",
                    frequency = ""
                ) }
            }

            is BoardEvent.SetId -> {
                _state.update {
                    it.copy(
                        id = event.id
                    )
                }
            }

            is BoardEvent.SetUser -> {
                _state.update {
                    it.copy(
                        user = event.user
                    )
                }
            }

            is BoardEvent.SetBoard -> {
                _state.update {
                    it.copy(
                        board = event.board
                    )
                }
            }

            is BoardEvent.SetFrequency -> {
                _state.update {
                    it.copy(
                        frequency = event.frequency
                    )
                }
            }

            BoardEvent.ShowDialog -> {
                _state.update {
                    it.copy(
                        isAddingBoard = true
                    )
                }
            }

            is BoardEvent.GetBoardById -> {
                GlobalScope.launch(Dispatchers.IO){
                    dao.getId(event.id).collectLatest() { a ->
                        if(a != null){
                            _state.update {
                                it.copy(
                                    id = a.id,
                                    user = a.user,
                                    board = a.board,
                                    frequency = a.frequency
                                )
                            }
                        }
                        Log.i("LECH", "WORKED")
                    }
                }
            }
        }
    }
}