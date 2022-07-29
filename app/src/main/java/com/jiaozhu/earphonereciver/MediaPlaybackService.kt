package com.jiaozhu.earphonereciver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.jiaozhu.earphonereciver.Model.Bean
import com.jiaozhu.earphonereciver.Model.SharedModel.dao
import com.jiaozhu.earphonereciver.Model.SharedModel.list
import com.jiaozhu.earphonereciver.comm.PrefSupport.Companion.context

private const val MY_MEDIA_ROOT_ID = "media_root_id"

class MediaPlaybackService : MediaBrowserServiceCompat() {

    companion object {
        private val CHANNEL_ID = "earphone"
        val PARAM_ACTION = "com.jiaozhu.EarphoneReceiver"
        val EXTRA_ACTION = "notify_action"
        val ACTION_PAUSE = "pause"
        val ACTION_NEXT = "next"
        val ACTION_CLOSE = "close"
        val ACTION_PLAY = "play"
    }

    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private lateinit var tts: TTsUtil
    var current: Bean? = null
    lateinit var builder: NotificationCompat.Builder


    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(baseContext, "LOG_TAG").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
            setPlaybackState(stateBuilder.build())
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setSessionToken(sessionToken)
            setCallback(callback2)
        }
        initTTs()
        createNotification()
    }


    private fun initTTs() {
        tts = TTsUtil(this)
        fun setItem(tag: String?, isPlaying: Boolean, isFinished: Boolean? = null): Int {
            val index = list.indexOfFirst { it.id == tag }
            current = list.getOrNull(index)?.apply {
                this.isPlaying = isPlaying
                isFinished?.let { this.isFinished = it }
            }
            return index
        }

        tts.listener = object : TTsUtil.TTsStatusListener {
            override fun onStatusChanged(tag: String?, status: PlaybackStateCompat) {
                when (status.state) {
                    PlaybackStateCompat.STATE_PLAYING -> {
                        setItem(tag, true)
                        list.find { it.id == tag }.let { builder.setSubText(it?.title) }
                    }
                    PlaybackStateCompat.STATE_PAUSED -> setItem(tag, false)

                    PlaybackStateCompat.STATE_STOPPED -> setItem(tag, false)
                    PlaybackStateCompat.STATE_NONE -> setItem(tag, false, true).apply {
                        list.getOrNull(this)?.let {
                            dao.replace(it)
                            list.remove(it)
                        }
                    }
                    PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> setItem(tag, false, true)
                    else -> {}
                }
                mNotificationManager.notify(1, builder.build())
            }

            override fun onPlaying(tag: String?, content: String?, index: Int) {
                builder.setContentText(content)
                current?.apply {
                    history = index
                    dao.updateHistory(id, index)
                }
                mNotificationManager.notify(1, builder.build())
            }
        }
    }


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val mediaItems = emptyList<MediaBrowserCompat.MediaItem>() as MutableList
        result.sendResult(mediaItems)
    }

    private val callback2 = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            tts.proper("tts",
                """构造 MediaBrowserCompat。传入您的 MediaBrowserService 的名称和您已定义的 MediaBrowserCompat.ConnectionCallback。onStart() 连接到MediaBrowserService。这里体现了 MediaBrowserCompat.ConnectionCallback 的神奇之处。如果连接成功，onConnect() 回调会创建媒体控制器，将其链接到媒体会话，"""
            )
            tts.isPlaying = true
        }

        override fun onStop() {
            tts.isPlaying = false
        }

        override fun onPause() {
            tts.isPlaying = false
        }
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "channel_name", NotificationManager.IMPORTANCE_LOW)
            channel.description = "channel_description"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }


        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause, "暂停",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next, "下一篇",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )
            .setContentIntent(mediaSession.controller.sessionActivity)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
            )
        startForeground(0, builder.build())
    }
}