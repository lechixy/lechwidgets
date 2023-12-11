package com.lechixy.lechwidgets.common

import com.lechixy.lechwidgets.database.BoardContent
import com.lechixy.lechwidgets.database.BoardPin
import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.lang.Exception

class PinterestUtil {

    object Events {
        const val UPDATE_PIN = "com.lechixy.lechwidgets.UPDATE_PIN"
    }

    companion object {
        val UPDATE_PIN_WIDGET_ID = "appWidgetId"
        val frequencyChoices = listOf("Every half hour", "Every hour", "Every 2 hours", "Every 3 hours")

        fun getFrequencyToLong(frequency: String?): Long{
            when(frequency){
                // Every half hour
                frequencyChoices[0] -> return 1_800_000
                // Every hour
                frequencyChoices[1] -> return 3_600_000
                // Every 2 hours
                frequencyChoices[2] -> return 7_200_000
                // Every 3 hours
                frequencyChoices[3] -> return 10_800_000
            }

            // Default is one hour
            return 3_600_000
        }

        fun getRssChannelContent(user: String, board: String): BoardContent? {
            val builder = RssParserBuilder()
            val rssParser = builder.build()
            var rssResult: RssChannel?
            var result: BoardContent? = null

            runBlocking(Dispatchers.IO) {
                rssResult = try {
                    rssParser.getRssChannel("https://tr.pinterest.com/${user}/${board}.rss")
                } catch (e: Exception) {
                    null
                }

                if (rssResult != null){
                    val contents = rssResult!!.items.map {
                        BoardPin(
                            it.title?.replace(",", "") ?: "",
                            it.link ?: "",
                            it.description?.replace(",", "") ?: "",
                            it.pubDate?.replace(",", "") ?: "",
                            it.image?.replace("236x", "564x") ?: ""
                        )
                    }
                    result = BoardContent(
                        contents,
                        rssResult!!.lastBuildDate ?: "",
                        rssResult!!.link ?: "",
                        rssResult!!.title ?: "",
                        "${user}/${board}"
                    )
                }
            }

            return result
        }
    }
}