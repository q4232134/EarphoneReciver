package com.jiaozhu.earphonereciver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jiaozhu.earphonereciver.Model.Bean
import com.jiaozhu.earphonereciver.Model.Support.db


/**
 * Created by 教主 on 2017/12/29.
 */
class TTsService : Service() {
    private val binder = TTSBinder()
    private lateinit var tts: TTsUtil
    var callback: TTSImpActivity? = null
    var mNotificationManager: NotificationManager? = null
    lateinit var builder: NotificationCompat.Builder
    private lateinit var receiver: BroadcastReceiver

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        initTTs()
        registerReceiver()
        createNotification()
    }


    override fun onDestroy() {
        binder.stop()
        binder.release()
        mNotificationManager?.cancel(1)
        mNotificationManager = null
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        binder.stop()
        binder.release()
        return super.onUnbind(intent)
    }

    internal inner class TTSBinder : Binder() {
        val isPlaying get() = tts.isPlaying
        val tag get() = tts.tag
        val service = this@TTsService

        fun start(isNext: Boolean = tts.isFinished) {
            if (isNext) {
                val item = list.firstOrNull { !it.isFinished } ?: return
                tts.proper(item.id, item.content)
            }
            tts.isPlaying = true
        }

        fun pause() {
            tts.isPlaying = false
        }

        fun stop() {
            tts.stop()
        }

        fun release() {
            tts.release()
        }

    }


    private fun initTTs() {
        tts = TTsUtil(this)
        fun setItem(tag: String?, isPlaying: Boolean, isFinished: Boolean? = null): Int {
            val index = list.indexOfFirst { it.id == tag }
            list.getOrNull(index)?.apply {
                this.isPlaying = isPlaying
                isFinished?.let { this.isFinished = it }
            }
            return index
        }
        tts.listener = object : TTsUtil.TTsListener {
            override fun onFinish(tag: String?) {
                setItem(tag, false, true).apply {
                    list.getOrNull(this)?.let { dao.replace(it) }
                    callback?.onItemChanged(this)
                }
                binder.start()
            }

            override fun onPause(tag: String?) {
                setItem(tag, false).apply { callback?.onItemChanged(this) }
                mNotificationManager?.notify(1, builder.build())
            }

            override fun onStart(tag: String?) {
                setItem(tag, true).apply { callback?.onItemChanged(this) }
                list.find { it.id == tag }.let { builder.setSubText(it?.title) }
                mNotificationManager?.notify(1, builder.build())
            }

            override fun onCancel(tag: String?) {
                setItem(tag, false).apply { callback?.onItemChanged(this) }
            }

            override fun onNext(tag: String?) {
                setItem(tag, false, true).apply { callback?.onItemChanged(this) }
                binder.start(true)
            }

            override fun onPlaying(tag: String?, content: String?) {
                builder.setContentText(content)
                mNotificationManager?.notify(1, builder.build())
            }
        }
    }


    fun List<Bean>.get(tag: String?): Bean? {
        return this.firstOrNull { it.id == tag }
    }

    fun getMatchFromString(str: String) {
        val rules = str.split("\n")
    }

    companion object {
        private val dao = db.dao()
        var list: MutableList<Bean> = dao.getActiveBean()
        private val CHANNEL_ID = "channel1"
        val ACTION_PAUSE = "pause"
        val ACTION_NEXT = "next"
        val ACTION_CLOSE = "close"
        val ACTION_PLAY = "play"
        val PARAM_ACTION = "com.jiaozhu.EarphoneReceiver"
        val EXTRA_ACTION = "notify_action"

        interface TTSImpActivity {
            fun onItemChanged(position: Int)
        }
    }


    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "channel_name", NotificationManager.IMPORTANCE_LOW)
            channel.description = "channel_description"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }


        /**
         * 获取对应的action
         * @param   action  执行的动作
         */
        fun getButtonAction(action: String) = Intent().setAction(action)
                .let { PendingIntent.getBroadcast(this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT) }

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(android.R.drawable.ic_media_pause, "暂停", getButtonAction(ACTION_PAUSE))
                .addAction(android.R.drawable.ic_media_next, "下一篇", getButtonAction(ACTION_NEXT))
//                .setStyle(NotificationCompat.MediaStyle().setShowActionsInCompactView(0))
                .setContentIntent(Intent(this, ListActivity::class.java).let { PendingIntent.getActivity(this, 0, it, 0) })
    }

    private fun registerReceiver() {
        receiver = MediaReceiver()
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PLAY)
            addAction(ACTION_CLOSE)
        }
        registerReceiver(receiver, filter)
    }


    inner class MediaReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            println(intent.action)
            when (intent.action) {
                ACTION_PAUSE -> {
                    if (binder.isPlaying)
                        binder.pause()
                    else
                        binder.start(false)
                }
                ACTION_NEXT -> binder.start(true)
                ACTION_PLAY -> binder.start(false)
                else -> ""
            }
        }
    }
}