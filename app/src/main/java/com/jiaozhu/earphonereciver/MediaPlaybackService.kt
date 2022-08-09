package com.jiaozhu.earphonereciver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.jiaozhu.earphonereciver.comm.PrefSupport.Companion.context
import com.jiaozhu.earphonereciver.model.Bean
import com.jiaozhu.earphonereciver.model.SharedModel.dao
import com.jiaozhu.earphonereciver.model.SharedModel.list
import java.io.File


private const val MY_MEDIA_ROOT_ID = "media_root_id"

class MediaPlaybackService : MediaBrowserServiceCompat() {

    companion object {
        private val CHANNEL_ID = "earphone"
        private val NOTIFICATION_ID = 1321
    }

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var tts: TTsUtil

    private var current: Bean? = null
    val isPlaying: Boolean get() = mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING


    override fun onCreate() {
        super.onCreate()
        initTTs()
        mediaSession = MediaSessionCompat(applicationContext, "MediaService").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(tts.stateBuilder.build())
            setSessionToken(sessionToken)
            setSessionActivity(PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, ListActivity::class.java), 0))
        }
        mediaSession.setCallback(mediaSessionCallback)
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON, null, applicationContext, MediaButtonReceiver::class.java)
        mediaSession.setMediaButtonReceiver(PendingIntent.getBroadcast(applicationContext, 0, mediaButtonIntent, 0))
        mediaSession.isActive = true //激活
        initFocus()
    }

    private fun initFocus() {
        /**
         * 媒体焦点改变监听器
         */
        val afChangeListener = OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    mediaSession.controller.transportControls.pause()
                AudioManager.AUDIOFOCUS_GAIN -> mediaSession.controller.transportControls.play()
            }
        }
        audioFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(afChangeListener)
                .setAudioAttributes(
                    AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build()
                )
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(true)
                .build()
        val notificationChannel = NotificationChannel(CHANNEL_ID, "Player controls", NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }


    private fun initTTs() {
        tts = TTsUtil(this)

        tts.listener = object : TTsUtil.TTsStatusListener {
            override fun onStatusChanged(tag: String?, status: PlaybackStateCompat) {
                current = list.find { it.id == tag }
                mediaSession.setPlaybackState(status)
                when (status.state) {
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
                refreshNotificationAndForegroundStatus(status)
            }

            override fun onPlaying(tag: String?, status: PlaybackStateCompat) {
                current?.apply {
                    history = status.position.toInt()
                    dao.updateHistory(id, status.position.toInt())
                }
                refreshNotificationAndForegroundStatus(status)
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        tts.release()
    }


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    /**
     * 获取媒体内容列表
     */
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val mediaItems = emptyList<MediaBrowserCompat.MediaItem>() as MutableList
        result.sendResult(mediaItems)
    }


    private lateinit var audioFocusRequest: AudioFocusRequest


    /**
     * 媒体操作回调
     */
    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            println(mediaButtonEvent)
            println(mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)?.keyCode)
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            println("onPlayFromMediaId")
        }

        override fun onPlay() {
            println("onPlay")
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val result = am.requestAudioFocus(audioFocusRequest)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                startService(Intent(context, MediaBrowserService::class.java))
//                registerAudioNoisyReceiver()
                if (current == null) prepare()
                mediaSession.isActive = true
                tts.play()
            }
        }


        override fun onStop() {
            println("onStop")
            tts.stop()
            mediaSession.isActive = false
            stopSelf()
//            unregisterAudioNoisyReceiver()
        }

        override fun onPause() {
            println("onPause")
            tts.pause()
//            unregisterAudioNoisyReceiver()
        }

        override fun onSkipToNext() {
            println("onSkipToNext")
            if (list.size == 0) return
            var index = list.indexOf(current) + 1
            if (index >= list.size) index = 0
            onStop()
            prepare(index)
            onPlay()
        }

        fun prepare(index: Int = 0) {
            if (list.size == 0) return
            current = list[index]
            with(current!!) {
                tts.proper(id, content, history)
            }
            mediaSession.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, current?.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, current?.id)
                    .build()
            )
        }
    }

    /**
     * 创建通知面板
     */
    private fun refreshNotificationAndForegroundStatus(status: PlaybackStateCompat) {
        val playbackState = status.state
        when (playbackState) {
            PlaybackStateCompat.STATE_PLAYING -> startForeground(
                NOTIFICATION_ID,
                getNotification(status)
            )
            PlaybackStateCompat.STATE_PAUSED -> {
                NotificationManagerCompat.from(this@MediaPlaybackService).notify(
                    NOTIFICATION_ID,
                    getNotification(status)
                )
                stopForeground(false)
            }
            else -> stopForeground(true)
        }
    }

    private fun getNotification(status: PlaybackStateCompat): Notification {
        val playbackState = status.state
        val controller = mediaSession.controller
        val mediaMetadata = controller?.metadata
        val description = mediaMetadata?.description


        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
        if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    "pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PAUSE
                    )
                )
            )
        } else {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_play,
                    "play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY
                    )
                )
            )
        }

        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                "next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
            )
        )


        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
                .setMediaSession(mediaSession.sessionToken)
        )

        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.color = ContextCompat.getColor(this, R.color.colorPrimaryDark)
        builder.setShowWhen(false)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        builder.setOnlyAlertOnce(true)
        builder.setChannelId(CHANNEL_ID)

        builder
            .setContentTitle(description!!.title)
            .setContentText(status.extras?.getString("content"))
            .setSubText(description.description)
            .setLargeIcon(description.iconBitmap)
            .setContentIntent(controller.sessionActivity)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        return builder.build()
    }


    /**
     * 注册噪音监听，断开耳机自动暂停
     */
    private val AUDIO_NOISY_INTENT_FILTER = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var mAudioNoisyReceiverRegistered = false
    private val mAudioNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                if (isPlaying) {
                    mediaSession.controller.transportControls.pause()
                }
            }
        }
    }

    private fun registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            this.registerReceiver(
                mAudioNoisyReceiver,
                AUDIO_NOISY_INTENT_FILTER
            )
            mAudioNoisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            context.unregisterReceiver(mAudioNoisyReceiver)
            mAudioNoisyReceiverRegistered = false
        }
    }

}