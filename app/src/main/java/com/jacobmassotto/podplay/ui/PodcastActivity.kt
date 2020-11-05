package com.jacobmassotto.podplay.ui

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.jacobmassotto.podplay.R
import com.jacobmassotto.podplay.adapter.PodcastListAdapter
import com.jacobmassotto.podplay.adapter.PodcastListAdapter.PodcastListAdapterListener
import com.jacobmassotto.podplay.db.PodPlayDatabase
import com.jacobmassotto.podplay.repository.ItunesRepo
import com.jacobmassotto.podplay.repository.PodcastRepo
import com.jacobmassotto.podplay.service.FeedService
import com.jacobmassotto.podplay.service.ItunesService
import com.jacobmassotto.podplay.viewmodel.PodcastViewModel
import com.jacobmassotto.podplay.viewmodel.SearchViewModel
import com.jacobmassotto.podplay.worker.EpisodeUpdateWorker
import kotlinx.android.synthetic.main.activity_podcast.*
import java.util.concurrent.TimeUnit

class PodcastActivity : AppCompatActivity(), PodcastListAdapterListener,
    PodcastDetailsFragment.OnPodcastDetailsListener {

    val TAG = javaClass.simpleName
    //pg 480
    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    //pg 501
    private lateinit var searchMenuItem: MenuItem
    //pg 503
    private val podcastViewModel by viewModels<PodcastViewModel>()

    //pg 500
    companion object {
        //Tag to identify the details Fragment in the Fragment Manager
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        //pg 568
        //Defines a name for the work request
        private const val TAG_EPISODE_UPDATE_JOB = "com.raywenderlich.podplay.episodes"
        //pg 619
        private const val TAG_PLAYER_FRAGMENT = "PlayerFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        updateControls()
        handleIntent(intent)
        addBackStackListener()
        setupPodcastListView()
        scheduleJobs()
    }

    //pg 468 (pdf)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //Inflate the options menu
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)
        //The search action menu item is found
        searchMenuItem = menu.findItem(R.id.search_item)
        //pg 552
        searchMenuItem.setOnActionExpandListener(object :
        MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
               return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                showSubscribedPodcasts()
                return true
            }
        })

        val searchView = searchMenuItem.actionView as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager               //Load the system SearchManager object. Adds the key functionality
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))                //Use searchManager to load search configuration and load it to searchView

        //pg 507
        if (supportFragmentManager.backStackEntryCount > 0) {
            podcastRecyclerView.visibility = View.INVISIBLE
        }


        //pg 502
        if (podcastRecyclerView.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }
        return true
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {                                                //Checks if the intent is an ACTION_SEARCH
            val query = intent.getStringExtra(SearchManager.QUERY) ?: return
            performSearch(query)
        }
        //pg 571
        val podcastFeedUrl =
            intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
        if (podcastFeedUrl != null) {
            podcastViewModel.setActivePodcast(podcastFeedUrl) {
                it?.let { podcastSummaryView ->
                    onShowDetails(podcastSummaryView)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    //pg 472 (pdf)
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    //pg 480
    private fun setupViewModels() {                                                                 //Set up view models for the Activity
        val service = ItunesService.instance
        searchViewModel.iTunesRepo = ItunesRepo(service)
        //pg 540
        val rssService = FeedService.instance                                                       //Creates a new instance of FeedService. Uses it to create a PodcastRepo object
        val db = PodPlayDatabase.getInstance(this)
        val podcastDao = db.podcastDao()
        podcastViewModel.podcastRepo = PodcastRepo(rssService, podcastDao)
    }

    private fun updateControls() {                                                                  //Set up RecyclerView with a PodcastListAdapter
        podcastRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        podcastRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            podcastRecyclerView.context, layoutManager.orientation
        )
        podcastRecyclerView.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(null, this, this)
        podcastRecyclerView.adapter = podcastListAdapter
    }

    //pg 481
    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {    //Called when user taps on a podcast in the RecylcerView
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
        showProgressBar()
        podcastViewModel.getPodcast(podcastSummaryViewData) {
            hideProgressBar()
            if (it != null) {
                showDetailsFragment()
            } else {
                showError("Error loading feed $feedUrl")
            }
        }
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }


    private fun performSearch(term: String) {
        showProgressBar()
        searchViewModel.searchPodcasts(term) { results ->
            hideProgressBar()
            toolbar.title = term
            podcastListAdapter.setSearchData(results)
        }
    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {

        var podcastDetailsFragment = supportFragmentManager                                         //Check and see if the fragment already exists
            .findFragmentByTag(TAG_DETAILS_FRAGMENT) as
                PodcastDetailsFragment?

        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()                           //If it doesn't create a new instance
        }
        return podcastDetailsFragment
    }

    //pg 501
    private fun showDetailsFragment() {
        val podcastDetailsFragment = createPodcastDetailsFragment()
        supportFragmentManager.beginTransaction().add(
            R.id.podcastDetailsContainer,
            podcastDetailsFragment, TAG_DETAILS_FRAGMENT)
            .addToBackStack("DetailsFragment").commit()
        podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    //pg 502
    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    //pg 505
    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    //pg 545
    override fun  onSubscribe() {                                                                   //Use the view model to save the active podcast
        podcastViewModel.saveAcivePodcast()
        supportFragmentManager.popBackStack()                                                       //Removes the PodcastDetailsFragment
    }

    //pg 546
    private fun showSubscribedPodcasts() {
        val podcasts = podcastViewModel.getPodcasts()?.value                                        //get the podcasts LiveData object
        if (podcasts != null) {                                                                     //If not null, update the podcast list adapter
            toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    private fun setupPodcastListView() {                                                            //Called when the Activity is created
        podcastViewModel.getPodcasts()?.observe(this, Observer {                             //Calls getPodcasts() and obeserves for any changes
            if (it != null) {
                showSubscribedPodcasts()
            }
        })
    }

    //pg 552
    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    //pg 568
    private fun scheduleJobs() {                                                                    //Start a work request from WorkManager
        val constraints: Constraints = Constraints.Builder().apply {                                //Required conditions for work request
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
        }.build()
        val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
            1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TAG_EPISODE_UPDATE_JOB, ExistingPeriodicWorkPolicy.REPLACE, request)
    }

    //pg 618
    override fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData) {
        podcastViewModel.activeEpisodeViewData = episodeViewData
        showPlayerFragment()
    }

    //pg 619
    private fun createEpisodePlayerFragment(): EpisodePlayerFragment
    {
        var episodePlayerFragment =

            supportFragmentManager.findFragmentByTag(TAG_PLAYER_FRAGMENT) as
                    EpisodePlayerFragment?
        if (episodePlayerFragment == null) {
            episodePlayerFragment = EpisodePlayerFragment.newInstance()
        }
        return episodePlayerFragment
    }

    private fun showPlayerFragment() {
        val episodePlayerFragment = createEpisodePlayerFragment()
        supportFragmentManager.beginTransaction().replace(
            R.id.podcastDetailsContainer,
            episodePlayerFragment,
            TAG_PLAYER_FRAGMENT).addToBackStack("PlayerFragment").commit()
        podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }
}
