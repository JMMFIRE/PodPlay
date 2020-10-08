package com.raywenderlich.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.util.DateUtils
import com.raywenderlich.podplay.util.DateUtils.dateToShortDate
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import java.util.*

//pg 494
class PodcastViewModel(application: Application) : AndroidViewModel (application) {

    var podcastRepo: PodcastRepo? = null
    var activePodcastViewData: PodcastViewData? = null
    //pg 542
    private var activePodcast: Podcast? = null
    //pg 543
    var livePodcastData: LiveData<List<SearchViewModel.PodcastSummaryViewData>>? = null
    //pg 619
    var activeEpisodeViewData: EpisodeViewData? = null

    data class PodcastViewData(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<EpisodeViewData>
    )

    data class EpisodeViewData (
        var guid: String? = "",
        var title: String? = "",
        var description: String? = "",
        var mediaUrl: String? = "",
        var releaseDate: Date? = null,
        var duration: String? = "",
        var isVideo: Boolean = false
        )

    //pg 495
    private fun episodesToEpisodesView(episodes: List<Episode>): List<EpisodeViewData> {            //Converts list of Episode models from repo to EpisodeViewData view models
        return episodes.map {
            val isVideo = it.mimeType.startsWith("video")
            EpisodeViewData(it.guid, it.title, it.description,
                it.mediaUrl, it.releaseDate, it.duration, isVideo)
        }
    }

    private fun podcastToPodcastView(podcast: Podcast) : PodcastViewData {                          //Converts a Podcast model to a PodcastViewData object
        return PodcastViewData(
            podcast.id != null,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDesc,
            podcast.imageUrl,
            episodesToEpisodesView(podcast.episodes)
        )}

    fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData,                  //Retrieves a podcast from the repo
                   callback: (PodcastViewData?) -> Unit) {
        val repo = podcastRepo ?: return
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
        repo.getPodcast(feedUrl) {
            it?.let {
                it.feedTitle = podcastSummaryViewData.name ?: ""
                it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                activePodcastViewData = podcastToPodcastView(it)
                activePodcast = it
                callback(activePodcastViewData)

            }
        }
    }

    //pg 542
    fun saveAcivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let { repo.save(it) }
    }

    private fun podcastToSummaryView(podcast: Podcast): SearchViewModel.PodcastSummaryViewData {
        return SearchViewModel.PodcastSummaryViewData(
            podcast.feedTitle,
            DateUtils.dateToShortDate(podcast.lastUpdated),
            podcast.imageUrl,
            podcast.feedUrl)
    }

    //pg 543
    fun getPodcasts(): LiveData<List<SearchViewModel.PodcastSummaryViewData>>? {
        val repo = podcastRepo ?: return null

        if (livePodcastData == null) {                                                              //Create a new livePodcastData if null
            val liveData = repo.getAll()                                                            //Retrieves all the assets from the podcast repo
            livePodcastData = Transformations.map(liveData)                                         //Convert the list into a LiveData object
            {podcastList ->
                podcastList.map { podcast ->
                    podcastToSummaryView(podcast)
                }
            }
        }
        return livePodcastData
    }

    //pg 551
    fun deleteActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            repo.delete(it)
        }
    }

    //pg 570
    fun setActivePodcast(feedUrl: String, callback:                                                 //Loads podcast from the database based on the feedUrl
        (SearchViewModel.PodcastSummaryViewData?) -> Unit) {
        val repo = podcastRepo ?: return
        repo.getPodcast(feedUrl) {
            if (it == null) {
                callback(null)
            } else {
                activePodcastViewData = podcastToPodcastView(it)
                activePodcast = it
                callback(podcastToSummaryView(it))
            }
        }
    }
}
