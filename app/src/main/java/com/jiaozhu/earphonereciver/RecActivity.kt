package com.jiaozhu.earphonereciver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.jiaozhu.earphonereciver.Model.Support.db
import dealString

/**
 * Created by 教主 on 2018/1/25.
 */
public class RecActivity : Activity() {
    companion object {
        private val dao = db.dao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        dealString(text.toString().replace("\\n", "\n"), dao)
        finish()
    }


}