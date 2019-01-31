package com.jiaozhu.earphonereciver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import android.widget.Toast
import androidx.media.session.MediaButtonReceiver
import java.util.*
import kotlin.properties.Delegates.observable


class TTsUtil(val context: Context) : TextToSpeech.OnInitListener, AudioManager.OnAudioFocusChangeListener {
    var tag: String? = null
    var listener: TTsListener? = null
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private val list: MutableList<String> = LinkedList()
    private var current = 0
    private val cacheLength = 1//缓存文本数量
    private var session: MediaSessionCompat? = null
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
    val handle: Handler = Handler(context.mainLooper)

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(p0: String) {
                current = p0.toInt()
                speak(current + cacheLength + 1)
                if (isFinished) {
                    isPlaying = false
                    handle.post {
                        listener?.onFinish(tag)
                    }
                }
            }

            override fun onError(p0: String) {
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (stopNotify) return
                stopNotify = true
                handle.post {
                    listener?.onPause(tag)
                }
            }

            override fun onStart(p0: String) {
                listener?.onPlaying(tag, list.getOrNull(p0.toInt()), p0.toInt())
                if (startNotify) return
                startNotify = true
                handle.post {
                    listener?.onStart(tag)
                }
            }
        })
        initMedia()
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
        getFocus()
    }

    /**
     * 开始
     */
    private fun start() {
        tts.stop()
        for (i in 0..cacheLength) {
            speak(current + i)
        }
        getFocus()
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
        listener?.onCancel(tag)
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
        unRegisterMediaButton()
    }


    interface TTsListener {
        /**
         * 播放结束
         */
        fun onFinish(tag: String?) {}

        /**
         * 取消播放
         */
        fun onCancel(tag: String?) {}

        /**
         * 开始播放
         */
        fun onStart(tag: String?) {}

        /**
         * 正在播放内容
         */
        fun onPlaying(tag: String?, content: String?, index: Int) {}

        /**
         * 暂停
         */
        fun onPause(tag: String?) {}

        /**
         * 需求下一条
         */
        fun onNext(tag: String?) {}
    }


    var wait = false//是否处于等待命令状态
    val switch = {
        if (wait) {
            isPlaying = !isPlaying
            wait = false
        }
    }


    private fun initMedia() {
        val mComponent = ComponentName(context.packageName, MediaButtonReceiver::class.java.name)
        session = MediaSessionCompat(context, context.packageName, mComponent, null)
        session?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(event: Intent): Boolean {
                val keyEvent: KeyEvent? = event.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                if (keyEvent?.action != KeyEvent.ACTION_DOWN) {
                    return false
                }
                when (keyEvent.keyCode) {//单击暂停，双击下一条
                    KeyEvent.KEYCODE_HEADSETHOOK -> {
                        if (wait) {
                            listener?.onNext(tag)
                            wait = false
                        } else {
                            wait = true
                            handle.postDelayed(switch, 750)
                        }
                    }

                }
                return true;
            }
        })
        session?.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        getFocus()
    }

    /**
     * 获取音频焦点
     */
    private fun getFocus() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        session?.isActive = true
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

    /**
     * 解除媒体按钮的注册
     */
    private fun unRegisterMediaButton() {
        session?.let {
            it.setCallback(null)
            it.isActive = false
            it.release()
        }
    }

    override fun onAudioFocusChange(p0: Int) {
        when (p0) {
            // 重新获得焦点,  可做恢复播放，恢复后台音量的操作
            AudioManager.AUDIOFOCUS_GAIN -> {
            }
            // 永久丢失焦点除非重新主动获取，这种情况是被其他播放器抢去了焦点，  为避免与其他播放器混音，可将音乐暂停
            AudioManager.AUDIOFOCUS_LOSS
            -> {
                isPlaying = false
            }
            // 暂时丢失焦点，这种情况是被其他应用申请了短暂的焦点，可压低后台音量
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
            -> {
                isPlaying = false
            }
            // 短暂丢失焦点，这种情况是被其他应用申请了短暂的焦点希望其他声音能压低音量（或者关闭声音）凸显这个声音（比如短信提示音），
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
            -> {
            }
        }
    }

}
