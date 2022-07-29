package com.jiaozhu.earphonereciver

import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import java.util.*
import kotlin.properties.Delegates.observable


class TTsUtil(val context: Context) : TextToSpeech.OnInitListener {
    var tag: String? = null
    var listener: TTsStatusListener? = null
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private val list: MutableList<String> = LinkedList()
    private var current = 0
    var mState: Int by observable(PlaybackStateCompat.STATE_NONE) { _, _, it ->
        listener?.apply {
            val stateBuilder = PlaybackStateCompat.Builder()
            stateBuilder.setState(mState, 0, 1.0f, SystemClock.elapsedRealtime())
            onStatusChanged(tag, stateBuilder.build())
        }
    }
    private val cacheLength = 1//缓存文本数量
    private var stopNotify = false//是否已发送停止通知标志，每次播放状态改变时重置，用于解决onStop调用多次的问题
    private var startNotify = false//是否已发送开始通知标志，每次播放状态改变时重置，用于解决onStart调用多次的问题
    var isPlaying by observable(false) { _, _, value ->
        stopNotify = false
        startNotify = false
        if (value) {
            start()
        } else {
            pause()
        }
    }
    val isFinished get() = current >= list.size - 1

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(p0: String) {
                current = p0.toInt()
                speak(current + cacheLength + 1)
                if (isFinished) {
                    isPlaying = false
                    mState = PlaybackStateCompat.STATE_NONE
                }
            }

            override fun onError(p0: String) {
                mState = PlaybackStateCompat.ERROR_CODE_APP_ERROR
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (stopNotify) return
                stopNotify = true
                mState = PlaybackStateCompat.STATE_PAUSED
            }

            override fun onStart(p0: String) {
                listener?.onPlaying(tag, list.getOrNull(p0.toInt()), p0.toInt())
                mState = PlaybackStateCompat.STATE_PLAYING
                if (startNotify) return
                startNotify = true
            }
        })
    }


    /**
     * 准备播放
     * @param   tag 标题
     * @param   content 内容
     */
    fun proper(tag: String?, content: String, history: Int = 0) {
        this.tag = tag
        dealTextMessage(content)
        current = history
    }

    /**
     * 开始
     */
    private fun start() {
        tts.stop()
        for (i in 0..cacheLength) {
            speak(current + i)
        }
    }

    /**
     * 暂停
     */
    private fun pause() {
        tts.stop()
    }

    /**
     * 结束
     */
    fun stop() {
        isPlaying = false
        list.clear()
        mState = PlaybackStateCompat.STATE_STOPPED
    }

    /**
     * 播放指定位置的文本
     * @return  播放是否成功
     */
    private fun speak(index: Int): Boolean {
        val str = list.getOrNull(index) ?: return false
        tts.speak(str, TextToSpeech.QUEUE_ADD, null, index.toString())
        return true
    }

    /**
     * 释放资源
     */
    fun release() {
        pause()
        tts.shutdown()
    }

    interface TTsStatusListener {
        fun onStatusChanged(tag: String?, status: PlaybackStateCompat)
        fun onPlaying(tag: String?, content: String?, index: Int) {}
    }

    /**
     * 处理文本信息
     */
    private fun dealTextMessage(share: String?) {
        list.clear()
        share?.split("\n", ",", "，", "。", "？", "?", "！", "!")?.filter { it.isNotEmpty() }?.forEach { list.add(it) }
    }


    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Toast.makeText(context, "无法启动语音合成", Toast.LENGTH_SHORT).show()
            return
        }
        tts.language = Locale.getDefault()
    }
}
