package com.jacobmassotto.podplay.service

data class PodcastResponse(
    val resultCount: Int,
    val results: List<ItunesPodcast>) {

    data class ItunesPodcast(                                                                       //Defines a data class that mirrors the layout
                                                                                                    // and hierarchy of the JSON data returned by iTunes
        val collectionCensoredName: String,
        val feedUrl: String,
        val artworkUrl100: String,
        val releaseDate: String
    )
}
