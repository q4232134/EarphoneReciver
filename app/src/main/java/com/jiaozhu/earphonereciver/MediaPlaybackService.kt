package com.jiaozhu.earphonereciver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.jiaozhu.earphonereciver.model.Bean
import com.jiaozhu.earphonereciver.comm.PrefSupport.Companion.context
import com.jiaozhu.earphonereciver.model.SharedModel.currentTag
import com.jiaozhu.earphonereciver.model.SharedModel.dao
import com.jiaozhu.earphonereciver.model.SharedModel.list

private const val MY_MEDIA_ROOT_ID = "media_root_id"

class MediaPlaybackService : MediaBrowserServiceCompat() {

    companion object {
        private val CHANNEL_ID = "earphone"
    }

    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private lateinit var tts: TTsUtil
    lateinit var builder: NotificationCompat.Builder
    private var current: Bean? = null


    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(baseContext, "LOG_TAG").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
            setPlaybackState(stateBuilder.build())
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setSessionToken(sessionToken)
            setCallback(callback2)
        }
        initTTs()
        createNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }


    private fun initTTs() {
        tts = TTsUtil(this)

        tts.listener = object : TTsUtil.TTsStatusListener {
            override fun onStatusChanged(tag: String?, status: PlaybackStateCompat) {
                current = list.find { it.id == tag }
                mediaSession.setPlaybackState(status)
                when (status.state) {
                    PlaybackStateCompat.STATE_PLAYING -> {
                        builder.setSubText(current?.title)
                    }
                    PlaybackStateCompat.STATE_NONE -> {
                        current?.let {
                            it.isFinished = true
                            dao.replace(it)
                            list.remove(it)
                        }
                        current = null
                        mediaSession.controller.transportControls.skipToNext()
                    }
                    PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> {
                        mediaSession.controller.transportControls.skipToNext()
                    }
                    else -> {}
                }
                currentTag = if (status.state == PlaybackStateCompat.STATE_PLAYING) current?.id else null
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

    override fun onDestroy() {
        super.onDestroy()
        tts.release()
    }


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val mediaItems = emptyList<MediaBrowserCompat.MediaItem>() as MutableList
        result.sendResult(mediaItems)
    }


    private lateinit var audioFocusRequest: AudioFocusRequest


    private val callback2 = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            println(mediaButtonEvent)
            //mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT).keyCode
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onPlay() {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                })
                build()
            }
            val result = am.requestAudioFocus(audioFocusRequest)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                startService(Intent(context, MediaBrowserService::class.java))
                if (current == null) prepare()
                tts.isPlaying = true
            }
        }

        override fun onStop() {
            tts.stop()
        }

        override fun onPause() {
            tts.isPlaying = false
        }

        override fun onSkipToNext() {
            if (list.size == 0) return
            var index = list.indexOf(current) + 1
            if (index >= list.size) index = 0
            prepare(index)
            onPlay()
        }

        fun prepare(index: Int = 0) {
            if (list.size == 0) return
            if (current == null) current = list[index]
            with(current!!) {
                tts.proper(id, content, history)
            }
            mediaSession.isActive = true
        }
    }

    private fun createNotification() {
        val channel = NotificationChannel(CHANNEL_ID, "channel_name", NotificationManager.IMPORTANCE_LOW)
        channel.description = "channel_description"
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)


        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause, "暂停",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next, "下一篇",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )
            .setContentIntent(mediaSession.controller.sessionActivity)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
            )
        startForeground(0, builder.build())
    }


}