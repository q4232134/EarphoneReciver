package com.jiaozhu.earphonereciver

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.jiaozhu.earphonereciver.Model.Bean
import com.jiaozhu.earphonereciver.Model.Dao
import getDao


/**
 * Created by 教主 on 2017/12/29.
 */
class TTsService : Service() {
    private val binder = TTSBinder()
    private lateinit var tts: TTsUtil
    var callback: TTSImpActivity? = null

    override fun onBind(intent: Intent): IBinder {
        initTTs()
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onDestroy() {
        binder.stop()
        binder.release()
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
                tts.proper(item.code, item.content)
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
            val index = list.indexOfFirst { it.code == tag }
            list.getOrNull(index)?.apply {
                this.isPlaying = isPlaying
                isFinished?.let { this.isFinished = it }
            }
            return index
        }
        tts.listener = object : TTsUtil.TTsListener {
            override fun onFinish(tag: String?) {
                println("onFinish")
                setItem(tag, false, true).apply {
                    list.getOrNull(this)?.let { dao.update(it, setOf("isFinished")) }
                    callback?.onItemChanged(this)
                }
                binder.start()
            }

            override fun onPause(tag: String?) {
                println("onPause")
                setItem(tag, false).apply { callback?.onItemChanged(this) }
            }

            override fun onStart(tag: String?) {
                println("onStart")
                setItem(tag, true).apply { callback?.onItemChanged(this) }
            }

            override fun onCancel(tag: String?) {
                println("onCancel")
                setItem(tag, false).apply { callback?.onItemChanged(this) }
            }

            override fun onNext(tag: String?) {
                setItem(tag, false, true).apply { callback?.onItemChanged(this) }
                binder.start(true)
            }
        }
    }


    fun List<Bean>.get(tag: String?): Bean? {
        return this.firstOrNull { it.code == tag }
    }

    fun getMatchFromString(str: String) {
        val rules = str.split("\n")
    }

    companion object {
        private val dao = getDao(Dao::class.java)
        var list: MutableList<Bean> = dao.getActiveBean()

        interface TTSImpActivity {
            fun onItemChanged(position: Int)
        }
    }
}