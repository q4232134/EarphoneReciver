package com.jiaozhu.earphonereciver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaMetadata
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.jiaozhu.earphonereciver.comm.PrefSupport.Companion.context
import com.jiaozhu.earphonereciver.model.Bean
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
    val isPlaying: Boolean get() = mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING


    override fun onCreate() {
        super.onCreate()
        val mbr = ComponentName(packageName, MediaButtonReceiver::class.java.name)
        mediaSession = MediaSessionCompat(this, "LOG_TAG", mbr, null).apply {
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
            setPlaybackState(stateBuilder.build())
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
            setSessionToken(sessionToken)
            setCallback(callback2)
        }
        initTTs()
        createNotification()
        registerAudioNoisyReceiver()
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
    private val callback2 = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            println(mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT).keyCode)
            when (mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT).keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    if (!isPlaying)
                        onPlay()
                    else
                        onPause()

                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    onSkipToNext()
                }
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        /**
         * 媒体焦点改变监听器
         */
        private val afChangeListener = OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    mediaSession.controller.transportControls.pause()
                AudioManager.AUDIOFOCUS_GAIN -> mediaSession.controller.transportControls.play()
            }
        }

        override fun onPlay() {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setOnAudioFocusChangeListener(afChangeListener).run {
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
//            unregisterAudioNoisyReceiver()
        }

        override fun onPause() {
            tts.isPlaying = false
//            unregisterAudioNoisyReceiver()
        }

        override fun onSkipToNext() {
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
            mediaSession.isActive = true
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
    private fun createNotification() {
        val channel = NotificationChannel(CHANNEL_ID, "阅读", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "语音阅读的媒体面板"
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(Intent(this, ListActivity::class.java).let { PendingIntent.getActivity(this, 0, it, 0) })
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
        startForeground(0, builder.build())
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