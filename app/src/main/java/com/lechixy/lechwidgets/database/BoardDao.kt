package com.lechixy.lechwidgets.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Upsert
    suspend fun upsertBoard(board: Board)

    @Delete
    suspend fun deleteBoard(board: Board)

    @Query("SELECT * FROM board")
    fun getAll(): Flow<List<Board>>

    @Query("SELECT * FROM board WHERE id LIKE :search")
    fun getId(search: Int): Flow<Board?>
}

@Dao
interface BoardContentDao {
    @Upsert
    suspend fun upsertBoardContent(board: BoardContent)

    @Delete
    suspend fun deleteBoardContent(board: BoardContent)

    @Query("SELECT * FROM boardcontent")
    fun getAll(): List<BoardContent>

    @Query("SELECT * FROM boardcontent WHERE `key` LIKE :search")
    fun getBoardContentByKey(search: String): BoardContent?
}