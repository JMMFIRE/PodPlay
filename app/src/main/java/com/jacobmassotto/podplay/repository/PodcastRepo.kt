package com.jacobmassotto.podplay.repository

import androidx.lifecycle.LiveData
import com.jacobmassotto.podplay.db.PodcastDao
import com.jacobmassotto.podplay.model.Episode
import com.jacobmassotto.podplay.model.Podcast
import com.jacobmassotto.podplay.service.FeedService
import com.jacobmassotto.podplay.service.RssFeedResponse
import com.jacobmassotto.podplay.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodcastRepo(private var feedService: FeedService,
                  private var podcastDao: PodcastDao) {

    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {
        //pg 548
        GlobalScope.launch {
            val podcast = podcastDao.loadPodcast(feedUrl)

            if (podcast != null) {                                                                  //Attempt to load the podcast from the DAO first
                podcast.id?.let {
                    podcast.episodes = podcastDao.loadEpisodes(it)
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            } else {

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

    //pg 551
    fun delete(podcast: Podcast) {                                                                  //Calls delete method in background
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }

    //pg 558
    private fun getNewEpisodes(localPodcast: Podcast, callBack: (List<Episode>) -> Unit) {          //Takes a subscribed podcast and downloads the latest episodes
        feedService.getFeed(localPodcast.feedUrl) { response ->                                     //Use feedService to download latest podcast episodes
            if (response != null) {

                val remotePodcast = rssResponseToPodcast(localPodcast.feedUrl,                      //Convert feed service response to the remotePodcast object
                    localPodcast.imageUrl, response)

                remotePodcast?.let {
                    val localEpisodes =                                                             //Load the list of local episodes from the database
                        podcastDao.loadEpisodes(localPodcast.id!!)

                    val newEpisodes = remotePodcast.episodes.filter {                               //Filter the remotePodcase episodes to contain only episodes not in the localEpisodes. Assign to newEpisodes
                            episode -> localEpisodes.find {
                            episode.guid == it.guid
                        }  == null
                    }
                    callBack(newEpisodes)
                }
            } else {
                callBack(listOf())
            }
        }
    }

    //pg 559
    private fun saveNewEpisodes(podcasId: Long, episodes: List<Episode>) {                          //Inserts the list of episodes into th database for the given podcastId
        GlobalScope.launch {
            for (episode in episodes) {
                episode.podcastId = podcasId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    //pg 559
    fun updatePodcastEpisodes(callback: (List<PodcastUpdateInfo>) -> Unit) {                        //Walks through all the subscribed podcasts and updates them with the latest episodes
        val updatedPodcasts: MutableList<PodcastUpdateInfo> = mutableListOf()                       //Initialize a list of empty PodcastUpdateInfo objects
        val podcasts = podcastDao.loadPodcastsStatic()                                              //Load subscribed podcasts from the database without LiveData wrapper
        var processCount = podcasts.count()

        for (podcast in podcasts) {
            getNewEpisodes(podcast) { newEpisodes ->
                if (newEpisodes.count() > 0) {
                    saveNewEpisodes(podcast.id!!, newEpisodes)

                    updatedPodcasts.add(PodcastUpdateInfo(podcast.feedUrl,
                        podcast.feedTitle, newEpisodes.count()))
                }
                processCount--
                if (processCount == 0) {
                    callback(updatedPodcasts)
                }
            }
        }
    }

    //pg 559
    class PodcastUpdateInfo (val feedUrl: String, val name: String, val newCount: Int)              //Used to hold update details for a single podcast
}
