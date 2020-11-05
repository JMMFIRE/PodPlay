package com.jacobmassotto.podplay.db

import android.content.Context
import androidx.room.*
import com.jacobmassotto.podplay.model.Episode
import com.jacobmassotto.podplay.model.Podcast
import java.util.*


//pg 538
//TypeConverters for Room database
class Converters {
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
//Defines the interface as a two table database, Podcast and Episode
@Database(entities = arrayOf(Podcast::class, Episode::class), version = 1)
//Tels Room to look in the Converters class to fin TypeConverters
@TypeConverters(Converters::class)
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