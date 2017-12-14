package com.jiaozhu.earphonereciver

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.properties.Delegates.observable


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, AudioManager.OnAudioFocusChangeListener {
    private lateinit var tts: TextToSpeech
    private val list: LinkedList<String> = LinkedList()
    private var session: MediaSessionCompat? = null
    private var isPlaying by observable(false) { _, _, value ->
        println(value)
        if (value) {
            start()
        } else {
            stop()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tts = TextToSpeech(this, this)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(p0: String) {
                list.removeAt(0)
            }

            override fun onError(p0: String) {
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
            }

            override fun onStart(p0: String) {
                getFocus()
            }
        })
        initMedia()
    }

    private fun initMedia() {
        getFocus()
        session = MediaSessionCompat(this, packageName, null, null)
        session?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(event: Intent): Boolean {
                val keyEvent: KeyEvent? = event.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                if (keyEvent?.action != KeyEvent.ACTION_DOWN) {
                    return false
                }
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_HEADSETHOOK -> isPlaying = !isPlaying
                }
                return true;
            }
        })
        session?.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        session?.isActive = true
    }

    /**
     * 获取音频焦点
     */
    private fun getFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }

    private fun stop() {
        tts.stop()
    }

    private fun start() {
        tts.stop()
        list.forEachIndexed { index, it -> tts.speak(it, TextToSpeech.QUEUE_ADD, null, index.toString()) }
    }


    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Toast.makeText(this, "无法启动语音合成", Toast.LENGTH_SHORT).show()
            return
        }
        tts.language = Locale.getDefault()
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        mText.setText(text)
        dealTextMessage(text)
    }

    /**
     * 处理文本信息
     */
    private fun dealTextMessage(share: String?) {
        list.clear()
        share?.split("\n", ",", "，", "。", "？", "！", "!")?.dropWhile({ it.isBlank() })?.forEach { list.add(it) }
        isPlaying = true
    }

    override fun onNewIntent(intent: Intent) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        dealTextMessage(text)
        mText.setText(text)
        super.onNewIntent(intent)

    }

    public override fun onDestroy() {
        stop()
        tts.shutdown()
        unRegisterMediaButton()
        super.onDestroy()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
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
                println("AUDIOFOCUS_GAIN")
            }
        // 永久丢失焦点除非重新主动获取，这种情况是被其他播放器抢去了焦点，  为避免与其他播放器混音，可将音乐暂停
            AudioManager.AUDIOFOCUS_LOSS
            -> {
                println("AUDIOFOCUS_LOSS")
                isPlaying = false
            }
        // 暂时丢失焦点，这种情况是被其他应用申请了短暂的焦点，可压低后台音量
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
            -> {
                println("AUDIOFOCUS_LOSS_TRANSIENT")
            }
        // 短暂丢失焦点，这种情况是被其他应用申请了短暂的焦点希望其他声音能压低音量（或者关闭声音）凸显这个声音（比如短信提示音），
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
            -> {
                println("AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
            }

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_read -> {
                dealTextMessage(mText.text.toString())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
