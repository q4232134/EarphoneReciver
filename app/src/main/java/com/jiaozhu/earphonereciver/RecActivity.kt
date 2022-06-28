package com.jiaozhu.earphonereciver

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jiaozhu.earphonereciver.Model.Support.db
import com.jiaozhu.earphonereciver.comm.PrefSupport
import dealString
import toast

/**
 * Created by 教主 on 2018/1/25.
 */
public class RecActivity : Activity() {
    companion object {
        private val dao = db.dao()
    }

    private lateinit var clipboard: ClipboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        dealString(text.toString().replace("\\n", "\n"), dao)
        finish()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
//        initClipboard()
        super.onDestroy()
    }

    private fun min(a: Int, b: Int): Int {
        if (a > b) return b
        return a
    }

    /**
     * 初始化剪切板监听
     */
    var text = ""

    private fun initClipboard() {
        clipboard = PrefSupport.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val temp = (clipboard.primaryClip?.getItemAt(0)?.text ?: "").toString()
        println("-----------------------$temp")
        if (temp.length < 50) {
            toast("文字少于50字符！")
            return
        }
        text = temp.substring(min(temp.length, 50))
        dealString(temp.replace("\\n", "\n"), dao)
    }
}