package com.raywenderlich.podplay.repository

import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.service.PodcastResponse
import retrofit2.Call
import retrofit2.Callback                                                                           //Don't be a dummy, import from retrofit, NOT MFING JAVA SECURITY
import retrofit2.Response
import com.raywenderlich.podplay.service.PodcastResponse.ItunesPodcast


//pg 458 (pdf)
class ItunesRepo(private val itunesService: ItunesService) {                                        //Define constructor to require an existing instance of ItunesService interface. Dependency Injection principle

    fun searchByTerm(term: String, callBack: (List<ItunesPodcast>?) -> Unit) {                      //Takes a search term and a method as parameters. Method defines a single List of ItunesPodcast objects

        val podcastCall = itunesService.searchPodcastByTerm(term)                                   //Call searchPodcastByTerm and pass the search term. Returns a Retrofit Call object

        podcastCall.enqueue(object : Callback<PodcastResponse> {                                    //enqueue runs in the background to retrieve the response from the web host. Takes a retrofit Callback interface
            override fun onFailure(call: Call<PodcastResponse>?,                                    //Part of the Callback interface. Called if anything goes wrong with the network
                                   t: Throwable?) {
                callBack(null)                                                                      //If error, call callBack() with null value
            }

            override fun onResponse(                                                                //If the network call succeeds
                call: Call<PodcastResponse>?,
                response: Response<PodcastResponse>?) {

                val body = response?.body()                                                         //Retrieve populated PodcastResponse model
                callBack(body?.results)                                                             //call callBack() with results object from PodcastResponse model
            }
        })
    }
}