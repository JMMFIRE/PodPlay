package com.jacobmassotto.podplay.repository

import com.jacobmassotto.podplay.service.ItunesService
import com.jacobmassotto.podplay.service.PodcastResponse
import retrofit2.Call
import retrofit2.Callback                                                                           //Don't be a dummy, import from retrofit, NOT MFING JAVA SECURITY
import retrofit2.Response
import com.jacobmassotto.podplay.service.PodcastResponse.ItunesPodcast


//pg 458 (pdf)
//Define constructor to require an existing instance of ItunesService interface. Dependency Injection principle
class ItunesRepo(private val itunesService: ItunesService) {

    //Takes a search term and a method as parameters. Method defines a single List of ItunesPodcast objects
    fun searchByTerm(term: String, callBack: (List<ItunesPodcast>?) -> Unit) {

        //Call searchPodcastByTerm and pass the search term. Returns a Retrofit Call object
        val podcastCall = itunesService.searchPodcastByTerm(term)

        //enqueue runs in the background to retrieve the response from the web host.
        // Takes a retrofit Callback interface
        podcastCall.enqueue(object : Callback<PodcastResponse> {
            //Part of the Callback interface. Called if anything goes wrong with the network
            override fun onFailure(call: Call<PodcastResponse>?,
                                   t: Throwable?) {
                //If error, call callBack() with null value
                callBack(null)
            }

            //If the network call succeeds
            override fun onResponse(
                call: Call<PodcastResponse>?,
                response: Response<PodcastResponse>?) {

                //Retrieve populated PodcastResponse model
                val body = response?.body()
                //call callBack() with results object from PodcastResponse model
                callBack(body?.results)
            }
        })
    }
}