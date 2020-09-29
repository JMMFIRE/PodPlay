package com.raywenderlich.podplay.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

//pg 533
@Entity(
    foreignKeys = [
        ForeignKey(                                                                                 //Define a single foreign key that relates the podcastId in the Episode entity to the property id in the Podcast entity
            entity = Podcast::class,                                                                //Defnes the parent entity
            parentColumns = ["id"],                                                                 //Defines the column names on the parent entity
            childColumns = ["podcastId"],                                                           //Defines the column names of child entity
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("podcastId")]
)

data class Episode (
    @PrimaryKey var guid: String = "",                                                              //Identifier provided in the RSS feed for an episode
    var podcastId: Long? = null,
    var title: String = "",
    var description: String = "",
    var mediaUrl: String = "",
    var mimeType: String = "",
    var releaseDate: Date = Date(),
    var duration: String = ""

)