package com.jiaozhu.earphonereciver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent

class MediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // 获得KeyEvent对象
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)

        if (Intent.ACTION_MEDIA_BUTTON == action) {

            // 获得按键码
            val keycode = event?.keyCode
            println(keycode)
            when (keycode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    println("KEYCODE_MEDIA_NEXT")
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    println("KEYCODE_MEDIA_PREVIOUS")
                }
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    println("KEYCODE_HEADSETHOOK")
                }
                else -> {
                }
            }//播放下一首
            //播放上一首
            //中间按钮,暂停or播放
            //可以通过发送一个新的广播通知正在播放的视频页面,暂停或者播放视频
        }
    }
}