package com.raywenderlich.podplay.ui

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.adapter.PodcastListAdapter.PodcastListAdapterListener
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.activity_podcast.*

class PodcastActivity : AppCompatActivity(),
    PodcastListAdapterListener,
    PodcastDetailsFragment.OnPodcastDetailsListener {

    val TAG = javaClass.simpleName
    //pg 480
    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    //pg 501
    private lateinit var searchMenuItem: MenuItem
    //pg 503
    private val podcastViewModel by viewModels<PodcastViewModel>()                                  //Used to hold the podcast details view data

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        updateControls()
        handleIntent(intent)
        addBackStackListener()
        setupPodcastListView()
    }

    //pg 468 (pdf)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater                                                                 //Inflate the options menu
        inflater.inflate(R.menu.menu_search, menu)
        searchMenuItem = menu.findItem(R.id.search_item)                                            //The search action menu item is found
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

    //pg 500
    companion object {
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"                                  //Tag to identify the details Fragment in the Fragment Manager
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
}
