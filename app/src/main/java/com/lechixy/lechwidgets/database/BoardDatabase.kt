package com.lechixy.lechwidgets.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@TypeConverters(TheTypeConverters::class)
@Database(
    entities = [Board::class, BoardContent::class],
    version = 3,
    exportSchema = false,
)
abstract class BoardDatabase: RoomDatabase() {

    abstract val boardDao: BoardDao
    abstract val boardContentDao: BoardContentDao

    companion object {
        private var instance: BoardDatabase? = null
        fun getInstance(context: Context): BoardDatabase {
            if (instance ==null) {
                instance = Room.databaseBuilder(context, BoardDatabase::class.java,"boards.db")
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return instance as BoardDatabase
        }
    }
}