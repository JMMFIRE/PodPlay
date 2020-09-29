package com.raywenderlich.podplay.ui

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.EpisodeListAdapter
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_podcast_details.*


//pg 497
class PodcastDetailsFragment : Fragment() {

    //pg 498
    private val podcastViewModel: PodcastViewModel by activityViewModels()                          //activityViewModels() is an extension function. Allows fragment to access and share view models from the parent activity
    //pg 525
    private lateinit var episodeListAdapter: EpisodeListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)                                                                     //Tells Android this fragment wants to add items to the options menu
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
        updateControls()
        setupControls()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
    }

    //pg 499
    private fun updateControls() {                                                                  //Uses view data to populate title and descriptions text view
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
            podcastViewModel.activePodcastViewData?.episodes)
        episodeRecyclerView.adapter = episodeListAdapter
    }
}