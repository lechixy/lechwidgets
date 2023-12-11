package com.lechixy.lechwidgets.database

import androidx.room.TypeConverter

class TheTypeConverters {
    @TypeConverter
    fun fromListToString(boardList: List<BoardPin>): String {
        return boardList.toString()
    }
    @TypeConverter
    fun fromStringToBoardContentList(stringList: String): List<BoardPin> {
        val result = mutableListOf<String>()
        val split = stringList.replace("[","").replace("]","").replace(" ","").split("),")
        for (n in split) {
            try {
                if(split.first() == n){
                    result.add("$n)")
                } else {
                    result.add(n)
                }
            } catch (e: Exception) {

            }
        }

        val boardContentList = mutableListOf<BoardPin>()
        result.toList().forEach {
            val formatted = it
                .replace("[","").replace("]","").replace(" ","").replace("BoardPin(", "")
                .replace(")", "").split(",")
            val valuesOfClass = mutableListOf<String>()
            formatted.forEach { value ->
                valuesOfClass.add(value.split("=")[1])
            }

            boardContentList.add(BoardPin("${valuesOfClass[0]}", "${valuesOfClass[1]}", "${valuesOfClass[2]}", "${valuesOfClass[3]}", "${valuesOfClass[4]}"))
        }

        return boardContentList.toList()
    }
}