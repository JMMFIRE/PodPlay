package com.raywenderlich.podplay.db

import android.content.Context
import androidx.room.*
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import java.util.*


//pg 538
class Converters {                                                                                  //TypeConverters for Room database
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return (date?.time)
    }
}

//pg 536
@Database(entities = arrayOf(Podcast::class, Episode::class), version = 1)                          //Defines the interface as a two table database, Podcast and Episode
@TypeConverters(Converters::class)                                                                  //Tels Room to look in the Converters class to fin TypeConverters
abstract class PodPlayDatabase : RoomDatabase() {

    abstract fun podcastDao(): PodcastDao

    companion object {
        private var instance: PodPlayDatabase? = null
        fun getInstance(context: Context): PodPlayDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(context.applicationContext,
                PodPlayDatabase::class.java, "PodPlayer").build()
            }
            return instance as PodPlayDatabase
        }
    }
}