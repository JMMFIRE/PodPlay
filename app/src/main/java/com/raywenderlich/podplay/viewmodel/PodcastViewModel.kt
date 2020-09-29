package com.raywenderlich.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.repository.PodcastRepo
import java.util.*

//pg 494
class PodcastViewModel(application: Application) : AndroidViewModel (application) {

    var podcastRepo: PodcastRepo? = null
    var activePodcastViewData: PodcastViewData? = null
    //pg 542
    private var activePodcast: Podcast? = null

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
        var duration: String? = ""
        )

    //pg 495
    private fun episodesToEpisodesView(episodes: List<Episode>): List<EpisodeViewData> {            //Converts list of Episode models from repo to EpisodeViewData view models
        return episodes.map {                                                                       //Map helps collect all variale into a list
            EpisodeViewData(it.guid, it.title, it.description,
                it.mediaUrl, it.releaseDate, it.duration)
        }
    }

    private fun podcastToPodcastView(podcast: Podcast) : PodcastViewData {                          //Converts a Podcast model to a PodcastViewData object
        return PodcastViewData(
            false,
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
}