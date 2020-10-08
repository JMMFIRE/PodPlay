package com.raywenderlich.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.service.PodcastResponse
import com.raywenderlich.podplay.util.DateUtils

//pg 474 (pdf)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    var iTunesRepo: ItunesRepo? = null                                                              //Property for an ItunesRepo to fetch information

    //pg 474
    data class PodcastSummaryViewData(
        var name: String? = "",
        var lastUpdated: String? = "",
        var imageUrl: String? = "",
        var feedUrl: String? = ""
    )

    private fun itunesPodcastToPodcastSummaryView(                                                  //Helper method to convert raw model data to the view data
        itunesPodcast: PodcastResponse.ItunesPodcast) : PodcastSummaryViewData {
        return PodcastSummaryViewData(
            itunesPodcast.collectionCensoredName,
            DateUtils.jsonDateToShortDate(itunesPodcast.releaseDate),
            itunesPodcast.artworkUrl100,
            itunesPodcast.feedUrl)
    }

    fun searchPodcasts(term: String, callback: (List<PodcastSummaryViewData>) -> Unit) {
        iTunesRepo?.searchByTerm(term) { results ->                                                 //iTunesRepo is used to perform the search asynchronously
            if (results == null) {
                callback(emptyList())                                                               //If results are null you pass an empty list to the callback method
            } else {
                val searchViews = results.map { podcast ->                                          //If results aren't null they are mapped out as PodcastSummaryViewData objects
                    itunesPodcastToPodcastSummaryView(podcast)
                }
                callback(searchViews)                                                               //Pass the results to the callback method so tey can be displayed
            }
        }
    }
}