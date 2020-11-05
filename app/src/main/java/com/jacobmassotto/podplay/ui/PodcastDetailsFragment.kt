package com.jacobmassotto.podplay.ui

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.jacobmassotto.podplay.R
import com.jacobmassotto.podplay.adapter.EpisodeListAdapter
import com.jacobmassotto.podplay.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_podcast_details.*


//pg 497
class PodcastDetailsFragment : Fragment(),
EpisodeListAdapter.EpisodeListAdapterListener {

    //pg 498
    //activityViewModels() is an extension function. Allows fragment
    // to access and share view models from the parent activity
    private val podcastViewModel: PodcastViewModel by activityViewModels()
    //pg 525
    private lateinit var episodeListAdapter: EpisodeListAdapter
    //pg 544
    private var listener: OnPodcastDetailsListener? = null
    //pg 550
    private var menuItem: MenuItem? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Tells Android this fragment wants to add items to the options menu
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_podcast_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        updateControls()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
        //pg 550
        menuItem = menu.findItem(R.id.menu_feed_action)
        updateMenuItem()
    }

    //pg 499
    //Uses view data to populate title and descriptions text view
    private fun updateControls() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        feedTitleTextView.text = viewData.feedTitle
        feedDescTextView.text = viewData.feedDesc
        activity?.let { activity ->
            Glide.with(activity).load(viewData.imageUrl)
                .into(feedImageView)
        }
    }

    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }

    //pg 525
    private fun setupControls() {
        feedDescTextView.movementMethod = ScrollingMovementMethod()
        episodeRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        episodeRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            episodeRecyclerView.context, layoutManager.orientation)
        episodeRecyclerView.addItemDecoration(dividerItemDecoration)

        episodeListAdapter = EpisodeListAdapter(
            podcastViewModel.activePodcastViewData?.episodes, this)
        episodeRecyclerView.adapter = episodeListAdapter
    }

    //pg 544
    interface OnPodcastDetailsListener {                                                            //Required parent activity to implement interface
        fun onSubscribe()
        fun onUnsubscribe()
        fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    override fun onAttach(context: Context) {                                                       //Called by Fragment anager when the fragment is attached
        super.onAttach(context)
        if (context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnPodcastDetails Listener")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {                                   //Called when a menu item is sleceted
        when (item.itemId) {
            R.id.menu_feed_action -> {                                                              //If the item id matches the subscribe button
                podcastViewModel.activePodcastViewData?.feedUrl?.let {
                    //pg 551
                    if (podcastViewModel.activePodcastViewData?.subscribed!!) {
                        listener?.onUnsubscribe()
                    } else {
                        listener?.onSubscribe()
                    }
                }
                return true
            } else ->   return super.onOptionsItemSelected(item)
        }
    }

    //pg 550
    private fun updateMenuItem() {
        val viewData = podcastViewModel.activePodcastViewData ?:
                return
        menuItem?.title = if (viewData.subscribed)
            getString(R.string.unsubscribe) else
        getString(R.string.subscribe)
    }

    //pg 583
    override fun onStop() {                                                                         //If the media controller is available and the mediaControllerCallback is not null the callabacks object is unregistered
        super.onStop()
            }

    override fun onStart() {
        super.onStart()
    }

    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        listener?.onShowEpisodePlayer(episodeViewData)
    }
}