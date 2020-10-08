package com.raywenderlich.podplay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.ui.PodcastActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

//pg 576
class PodplayMediaService : MediaBrowserServiceCompat(),
    PodplayMediaCallback.PodplayMediaListener {

    private lateinit var mediaSession: MediaSessionCompat

    //pg 585
    companion object {
        private const val PODPLAY_EMPTY_ROOT_MEDIA_ID =
            "podplay_empty_root_media_id"

        //pg 599
        private const val PLAYER_CHANNEL_ID = "podplay_player_channel"
        //pg 602
        private const val NOTIFICATION_ID = 1                                                       //Unique notification ID
    }

    override fun onCreate() {
        super.onCreate()
        createMediaSession()

    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        //pg 585
        if (parentId.equals(PODPLAY_EMPTY_ROOT_MEDIA_ID)) {
            result.sendResult(null)
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return MediaBrowserServiceCompat.BrowserRoot(
            PODPLAY_EMPTY_ROOT_MEDIA_ID, null
        )
    }

    //pg 577
    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(
            this,
            "PodplayMediaService"
        )                 //mediaSession property is initialized  with a new MediaSessionCompat object
        setSessionToken(mediaSession.sessionToken)                                                  //Retrieve unique media session token
        val callBack = PodplayMediaCallback(
            this,
            mediaSession
        )                             //Adds a new instance of callback method and sets it as the media session callback
        callBack.listener = this
        mediaSession.setCallback(callBack)
    }

    //pg 598
    private fun getPausePlayActions():                                                              //Create  pause and play actions and return them to the caller
            Pair<NotificationCompat.Action, NotificationCompat.Action> {
        val pauseAction = NotificationCompat.Action(
            R.drawable.ic_pause_white, getString(R.string.pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PAUSE
            )
        )
        val playAction = NotificationCompat.Action(
            R.drawable.ic_play_arrow_white, getString(R.string.play),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PLAY
            )
        )
        return Pair(pauseAction, playAction)
    }

    //pg 599
    private fun isPlaying(): Boolean {                                                              //Checks the current playback state. Returns true if playing
        if (mediaSession.controller.playbackState != null) {
            return mediaSession.controller.playbackState.state ==
                    PlaybackStateCompat.STATE_PLAYING
        } else {
            return false
        }
    }

    private fun getNotificationIntent(): PendingIntent {                                            //Creates pending intent that will open PodcastActivity
        val openActivityIntent = Intent(
            this,
            PodcastActivity::class.java
        )
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            this@PodplayMediaService, 0, openActivityIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
        if (notificationManager.getNotificationChannel
                (PLAYER_CHANNEL_ID) == null
        ) {
            val channel = NotificationChannel(
                PLAYER_CHANNEL_ID,
                "Player", NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    //pg 600
    private fun createNotification(mediaDescription:
                                   MediaDescriptionCompat,
                                   bitmap: Bitmap?): Notification {

        val notificationIntent = getNotificationIntent()                                            //Main notification intent
        val (pauseAction, playAction) = getPausePlayActions()                                       //Pause play actions
        val notification = NotificationCompat.Builder(                                              //Create notification builder using player channel ID
            this@PodplayMediaService, PLAYER_CHANNEL_ID)

        notification                                                                                //Notification builder
            .setContentTitle(mediaDescription.title)
            .setContentText(mediaDescription.subtitle)
            .setLargeIcon(bitmap)
            .setContentIntent(notificationIntent)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent
                    (this, PlaybackStateCompat.ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_episode_icon)
            .addAction(if (isPlaying()) pauseAction else playAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.
                        buildMediaButtonPendingIntent(
                            this, PlaybackStateCompat.ACTION_STOP)))

        return notification.build()
    }

    //pg 602
    private fun displayNotification() {

        if (mediaSession.controller.metadata == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val mediaDescription =
            mediaSession.controller.metadata.description

        GlobalScope.launch {
            val iconUrl = URL(mediaDescription.iconUri.toString())
            val bitmap =
                BitmapFactory.decodeStream(iconUrl.openStream())
            val notification = createNotification(mediaDescription,
                bitmap)

            ContextCompat.startForegroundService(
                this@PodplayMediaService,
                Intent(this@PodplayMediaService,
                    PodplayMediaService::class.java))

            startForeground(PodplayMediaService.NOTIFICATION_ID,
                notification)
        }
    }

    //pg 605
    override fun onStateChanged() {
        displayNotification()
    }

    override fun onStopPlaying() {
        stopSelf()
        stopForeground(true)
    }

    override fun onPausePlaying() {
        stopForeground(false)
    }

    //pg 607
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession.controller.transportControls.stop()
    }
}