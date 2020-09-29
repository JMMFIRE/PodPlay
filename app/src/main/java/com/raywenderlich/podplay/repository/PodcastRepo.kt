package com.raywenderlich.podplay.repository

import androidx.lifecycle.LiveData
import com.raywenderlich.podplay.db.PodcastDao
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.RssFeedResponse
import com.raywenderlich.podplay.service.RssFeedService
import com.raywenderlich.podplay.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodcastRepo(private var feedService: FeedService, private var podcastDao: PodcastDao) {
    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {
        // pg 523
        feedService.getFeed(feedUrl) { feedResponse ->
            var podcast: Podcast? = null
            if (feedResponse != null) {
                podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
            }
            GlobalScope.launch(Dispatchers.Main) {                                                  //Passes Dispatchers.Main context. Forces enclosing code to run on main thread
                callback(podcast)
            }
        }
    }

    //pg 521
    private fun rssItemsToEpisodes(episodeResponses:
                                   List<RssFeedResponse.EpisodeResponse>): List<Episode> {          //Uses the map method to convert a list of EpisodeResponse objects into a list of Episode objects
        return episodeResponses.map {
            Episode(
                it.guid ?: "",
                null,
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }

    //pg 522
    private fun rssResponseToPodcast(feedUrl: String, imageUrl: String,
                                     rssResponse: RssFeedResponse): Podcast? {
        val items = rssResponse.episodes ?: return null
        val description = if (rssResponse.description == "")
            rssResponse.summary else rssResponse.description
        return Podcast(null, feedUrl, rssResponse.title, description, imageUrl,
            rssResponse.lastUpdated, episodes = rssItemsToEpisodes(items))

    }

    //pg 540
    fun save(podcast:Podcast) {                                                                     //Uses podcastDao object to save a Podcast and it's episode to the db
        GlobalScope.launch {
            val podcastId = podcastDao.insertPodcast(podcast)
            for (episode in podcast.episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    //pg 541
    fun getAll(): LiveData<List<Podcast>>
    {
        return podcastDao.loadPodcasts()
    }
}
