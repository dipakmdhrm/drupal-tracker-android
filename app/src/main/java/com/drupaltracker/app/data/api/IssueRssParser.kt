package com.drupaltracker.app.data.api

import android.util.Xml
import com.drupaltracker.app.data.model.IssueApiModel
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.Locale

object IssueRssParser {

    fun parse(xmlBody: String): List<IssueApiModel> {
        val issues = mutableListOf<IssueApiModel>()
        val parser = Xml.newPullParser()
        parser.setInput(xmlBody.reader())

        var inItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        val currentText = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentText.clear()
                    if (parser.name == "item") {
                        inItem = true
                        title = ""; link = ""; pubDate = ""
                    }
                }
                XmlPullParser.TEXT -> currentText.append(parser.text)
                XmlPullParser.END_TAG -> {
                    if (inItem) {
                        when (parser.name) {
                            "title"   -> title   = currentText.toString().trim()
                            "link"    -> link     = currentText.toString().trim()
                            "pubDate" -> pubDate  = currentText.toString().trim()
                            "item"    -> {
                                val nid = link.trimEnd('/').substringAfterLast('/')
                                if (nid.isNotBlank() && title.isNotBlank()) {
                                    issues.add(IssueApiModel(
                                        nid = nid,
                                        title = title,
                                        changed = parsePubDateToSeconds(pubDate).toString(),
                                        url = link
                                    ))
                                }
                                inItem = false
                            }
                        }
                    }
                    currentText.clear()
                }
            }
            eventType = parser.next()
        }
        return issues
    }

    private fun parsePubDateToSeconds(pubDate: String): Long {
        if (pubDate.isBlank()) return 0L
        return try {
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
            (sdf.parse(pubDate)?.time ?: 0L) / 1000L
        } catch (e: Exception) {
            0L
        }
    }
}
