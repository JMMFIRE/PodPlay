package com.jacobmassotto.podplay.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.jacobmassotto.podplay.R
import com.jacobmassotto.podplay.service.PodplayMediaCallback
import com.jacobmassotto.podplay.service.PodplayMediaCallback.Companion.CMD_CHANGESPEED
import com.jacobmassotto.podplay.service.PodplayMediaCallback.Companion.CMD_EXTRA_SPEED
import com.jacobmassotto.podplay.service.PodplayMediaService
import com.jacobmassotto.podplay.util.HtmlUtils
import com.jacobmassotto.podplay.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_episode_player.*

//pg 618
class EpisodePlayerFragment : Fragment() {
    //pg 621
    private val podcastViewModel: PodcastViewModel by activityViewModels()
    //pg 623
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    //pg 630
    private var playerSpeed: Float = 1.0f
    //pg 634
    private var episodeDuration: Long = 0
    private var draggingScrubber: Boolean = false
    private var progressAnimator: ValueAnimator? = null
    //pg 642
    private var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playOnPrepare: Boolean = false
    //pg 647
    private var isVideo: Boolean = false

    companion object {
        fun newInstance(): EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }

    //pg 581
    inner class MediaBrowserCallBacks: MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            println("onConnected")
            updateControlsFromController()
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
            //disable transport controls
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
            //fatal error handling
        }
    }

    //pg 580
    inner class MediaControllerCallback: MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println(
                "metadata changed to ${metadata?.getString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")
            metadata?.let { updateControlsFromMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            println("state changed to $state")
            //pg 625
            val state = state ?: return
            handleStateChange(state.state, state.position,
                state.playbackSpeed)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        //pg 647
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isVideo = podcastViewModel.activeEpisodeViewData?.isVideo ?:
                    false
        } else {
            isVideo = false
        }
        if (!isVideo) {
            initMediaBrowser()
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View?{
        return inflater.inflate(
            R.layout.fragment_episode_player,
            container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        if (isVideo) {
            initMediaSession()
            initVideoPlayer()
        }
        updateControls()
    }

    override fun onStart() {
        super.onStart()
        if (!isVideo) {
            if (mediaBrowser.isConnected) {
                val fragmentActivity = activity as FragmentActivity
                if (MediaControllerCompat.getMediaController(
                        fragmentActivity) == null) {
                    registerMediaController(mediaBrowser.sessionToken)
                }
                updateControlsFromController()
            } else {
                mediaBrowser.connect()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        progressAnimator?.cancel()

        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity)
                    .unregisterCallback(it)
            }
        }
        //pg 648
        if (isVideo) {
            mediaPlayer?.setDisplay(null)
        }

        //pg 649
        if (!fragmentActivity.isChangingConfigurations) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    //pg 621
    private fun updateControls() {
        episodeTitleTextView.text = podcastViewModel.activeEpisodeViewData?.title
        val htmlDesc = podcastViewModel.activeEpisodeViewData?.description ?: ""
        val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)

        episodeDescTextView.text = descSpan
        episodeDescTextView.movementMethod = ScrollingMovementMethod()

        val fragmentActivity = activity as FragmentActivity
        Glide.with(fragmentActivity)
            .load(podcastViewModel.activePodcastViewData?.imageUrl)
            .into(episodeImageView)

        //pg 631
        speedButton.text = "${playerSpeed}x"

        //pg 649
        mediaPlayer?.let { updateControlsFromController() }
    }

    //pg 587
    private fun startPlaying(
        //Use media controller transport controls to initiate the media playback
        episodeViewData: PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)

        //pg 606
        val viewData = podcastViewModel.activePodcastViewData ?: return
        val bundle = Bundle()
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_TITLE,
            episodeViewData.title)
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_ARTIST,
            viewData.feedTitle)
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
            viewData.imageUrl)
        controller.transportControls.playFromUri(
            Uri.parse(episodeViewData.mediaUrl), bundle)
    }

    //pg 582
    //Instantiate a new MediaBrowserCompat object
    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(fragmentActivity,
            ComponentName(fragmentActivity,
                PodplayMediaService::class.java),
            MediaBrowserCallBacks(),
            null)
    }

    private fun registerMediaController(token: MediaSessionCompat.Token) {
        //Assign to activity
        val fragmentActivity = activity as FragmentActivity
        //Create MediaController and associate with the session token.
        //Connects media controller with the media session
        val mediaController = MediaControllerCompat(fragmentActivity, token)

        //assign media controller with the activity
        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    //pg 624
    private fun togglePlayPause() {
        playOnPrepare = true
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null) {
            if (controller.playbackState.state ==
                PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
            }
        } else {
            podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
        }
    }

    //pg 625
    private fun setupControls() {
        playToggleButton.setOnClickListener {
            togglePlayPause()
        }
        //pg 631
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            speedButton.setOnClickListener {
                changeSpeed()
            }
        } else {
            speedButton.visibility = View.INVISIBLE
        }

        //pg 633
        forwardButton.setOnClickListener {
            seekBy(30)
        }
        replayButton.setOnClickListener {
            seekBy(-10)
        }

        //pg 635
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress:
                Int, fromUser: Boolean) {
                    currentTimeTextView.text = DateUtils.formatElapsedTime(
                        (progress / 1000).toLong())
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    draggingScrubber = true
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    draggingScrubber = false
                    val fragmentActivity = activity as FragmentActivity
                    val controller = MediaControllerCompat.getMediaController(fragmentActivity)

                    if (controller.playbackState != null) {
                        // 6

                        controller.transportControls.seekTo(seekBar.progress.toLong())
                    } else {
                        // 7
                        seekBar.progress = 0
                    }
                }
            })
    }

    private fun handleStateChange(state: Int, position: Long, speed:
    Float) {
        progressAnimator?.let {
            it.cancel()
            progressAnimator = null
        }

        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        playToggleButton.isActivated = isPlaying

        //pg 639
        val progress = position.toInt()
        seekBar.progress = progress
        speedButton.text = "${playerSpeed}x"
        if (isPlaying) {
            if (isVideo) {
                setupVideoUI()
            }
            animateScrubber(progress, speed)
        }
    }

    //pg 630
    private fun changeSpeed() {
        playerSpeed += 0.25f
        if (playerSpeed > 2.0f) {
            playerSpeed = 0.75f
        }

        val bundle = Bundle()
        bundle.putFloat(CMD_EXTRA_SPEED, playerSpeed)
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        controller.sendCommand(CMD_CHANGESPEED, bundle, null)

        speedButton.text = "${playerSpeed}x"
    }

    //pg 632
    private fun seekBy(seconds: Int) {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        val newPosition = controller.playbackState.position +
                seconds*1000
        controller.transportControls.seekTo(newPosition)
    }

    //pg 634
    private fun updateControlsFromMetadata(metadata: MediaMetadataCompat) {
        episodeDuration =
            metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        endTimeTextView.text = DateUtils.formatElapsedTime(
            episodeDuration / 1000)
        seekBar.max = episodeDuration.toInt()
    }

    //pg 637
    private fun animateScrubber(progress: Int, speed: Float) {
        val timeRemaining = ((episodeDuration - progress) /
                speed).toInt()

        if (timeRemaining < 0) {
            return;
        }
        progressAnimator = ValueAnimator.ofInt(
            progress, episodeDuration.toInt())
        progressAnimator?.let { animator ->
            animator.duration = timeRemaining.toLong()
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (draggingScrubber) {
                    animator.cancel()
                } else {
                    seekBar.progress = animator.animatedValue as Int
                }
            }
            animator.start()
        }
    }

    //pg 640
    private fun updateControlsFromController() {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller != null) {
            val metadata = controller.metadata
            if (metadata != null) {
                handleStateChange(controller.playbackState.state,
                    controller.playbackState.position, playerSpeed)
                updateControlsFromMetadata(controller.metadata)
            }
        }
    }

    //pg 642
    private fun initMediaSession() {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(activity as Context,
                "EpisodePlayerFragment")
            mediaSession?.setMediaButtonReceiver(null)
        }
        registerMediaController(mediaSession!!.sessionToken)
    }

    //pg 643
    private fun setSurfaceSize() {
        val mediaPlayer = mediaPlayer ?: return
        val videoWidth = mediaPlayer.videoWidth
        val videoHeight = mediaPlayer.videoHeight
        val parent = videoSurfaceView.parent as View
        val containerWidth = parent.width
        val containerHeight = parent.height
        val layoutAspectRatio = containerWidth.toFloat() /
                containerHeight
        val videoAspectRatio = videoWidth.toFloat() / videoHeight
        val layoutParams = videoSurfaceView.layoutParams

        if (videoAspectRatio > layoutAspectRatio) {
            layoutParams.height =
                (containerWidth / videoAspectRatio).toInt()
        } else {
            layoutParams.width =
                (containerHeight * videoAspectRatio).toInt()
        }
        videoSurfaceView.layoutParams = layoutParams
    }

    //pg 644
    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.let {
                it.setAudioStreamType(AudioManager.STREAM_MUSIC)
                it.setDataSource(
                    podcastViewModel.activeEpisodeViewData?.mediaUrl)
                it.setOnPreparedListener {
                    val fragmentActivity = activity as FragmentActivity
                    val episodeMediaCallback = PodplayMediaCallback(
                        fragmentActivity, mediaSession!!, it)
                    mediaSession!!.setCallback(episodeMediaCallback)

                    setSurfaceSize()

                    if (playOnPrepare) {
                        togglePlayPause()
                    }
                }
                it.prepareAsync()
            }
        } else {
            setSurfaceSize()
        }
    }

    //pg 645
    private fun initVideoPlayer() {
        videoSurfaceView.visibility = View.VISIBLE
        val surfaceHolder = videoSurfaceView.holder

        surfaceHolder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                initMediaPlayer()
                mediaPlayer?.setDisplay(holder)
            }
            override fun surfaceChanged(var1: SurfaceHolder, var2: Int,
                                        var3: Int, var4: Int) {
            }
            override fun surfaceDestroyed(var1: SurfaceHolder) {
            }
        })
    }

    //pg 648
    private fun setupVideoUI() {
        episodeDescTextView.visibility = View.INVISIBLE
        headerView.visibility = View.INVISIBLE
        val activity = activity as AppCompatActivity
        activity.supportActionBar?.hide()
        playerControls.setBackgroundColor(Color.argb(255/2, 0, 0, 0))
    }
}