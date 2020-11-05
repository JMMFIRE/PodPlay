package com.jacobmassotto.podplay.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jacobmassotto.podplay.R
import com.jacobmassotto.podplay.db.PodPlayDatabase
import com.jacobmassotto.podplay.repository.PodcastRepo
import com.jacobmassotto.podplay.service.FeedService
import com.jacobmassotto.podplay.ui.PodcastActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

//pg 562
class EpisodeUpdateWorker(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

    //pg 566
    override suspend fun doWork(): Result = coroutineScope {
        val job = async {
            val db = PodPlayDatabase.getInstance(applicationContext)
            val repo = PodcastRepo(FeedService.instance, db.podcastDao())

            repo.updatePodcastEpisodes { podcastUpdates ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                }
                for (podcastUpdate in podcastUpdates) {
                    displayNotification(podcastUpdate)
                }
            }
        }
        job.await()
        Result.success()
    }


    //pg 563
    companion object {
        const val EPISODE_CHANNEL_ID = "podplay_episodes_channel"                                   //Channel id that identifies this channel to the notification system
        //pg 564
        const val EXTRA_FEED_URL = "PodcastFeedUrl"
    }

    @RequiresApi(Build.VERSION_CODES.O)                                                             //Notify the compiler method should ony be called with api 26 (Oreo) or newer
    private fun createNotificationChannel() {

        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as                            //Retrieve notification manager
                    NotificationManager

        if (notificationManager.getNotificationChannel(EPISODE_CHANNEL_ID) == null) {               //Notification manager checks if the channel already exists
            val channel = NotificationChannel(EPISODE_CHANNEL_ID,                                   //If channel doesn't exist the NotificationChannel object is created with name "Episodes"
            "Episodes", NotificationManager.IMPORTANCE_DEFAULT)

            notificationManager.createNotificationChannel(channel)
        }
    }

    //pg 565
    private fun displayNotification(podcastInfo:
          PodcastRepo.PodcastUpdateInfo) {
        val contentIntent = Intent(applicationContext, PodcastActivity::class.java)                 //Provides a pending intent for the notification.
        contentIntent.putExtra(EXTRA_FEED_URL, podcastInfo.feedUrl)

        val pendingContentIntent =                                                                  //When user taps banner, the PodcastActivity opens
            PendingIntent.getActivity(applicationContext, 0,
                contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notification =                                                                          //Sets the properties for the notification
            NotificationCompat.Builder(applicationContext, EPISODE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_episode_icon)
                .setContentTitle(applicationContext.getString(
                    R.string.episode_notification_title))
                .setContentText(applicationContext.getString(
                    R.string.episode_notification_text, podcastInfo.newCount,
                podcastInfo.name))
                .setNumber(podcastInfo.newCount)
                .setAutoCancel(true)
                .setContentIntent(pendingContentIntent)
                .build()
        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(podcastInfo.name, 0, notification)


    }
}