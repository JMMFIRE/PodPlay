package com.raywenderlich.podplay.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
//pg 482
object DateUtils {
    fun jsonDateToShortDate(jsonDate: String?): String {                                            //Used to parse the date handed by iTunes
        if (jsonDate == null) {
            return "-"
        }

        val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",                            //Define a date format that matches the date format returned by iTunes
        Locale.getDefault())
        val date = inFormat.parse(jsonDate) ?: return "-"
        val outputFormat = DateFormat.getDateInstance(DateFormat.SHORT,
        Locale.getDefault())
        return outputFormat.format(date)
    }

    fun xmlDateToDate(dateString: String?): Date {                                                  //Converts date string from RSS XML file to Date object
        val date = dateString ?: return Date()
        val inFormat = SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss z",
        Locale.getDefault())
        return inFormat.parse(date) ?: Date()
    }

    //pg 528
    fun datToShortDate(date: Date): String {
        val outputFormat = DateFormat.getDateInstance(
            DateFormat.SHORT, Locale.getDefault())
        return outputFormat.format(date)
    }
}