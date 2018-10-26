package com.jiaozhu.earphonereciver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.jiaozhu.earphonereciver.Model.Bean
import com.jiaozhu.earphonereciver.Model.Support.db
import com.jiaozhu.earphonereciver.comm.filtered
import toast

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
        dealString(text.toString().replace("\\n", "\n"))
        finish()
    }


    /**
     * 处理请求
     */
    private fun dealString(text: String?) {
        if (text.isNullOrEmpty()) return
        val model = Bean(text.filtered + "\n下一条")
        if (TTsService.list.contains(model)) {
            toast("条目已存在")
            return
        }
        TTsService.list.add(model)
        if (dao.replace(model) > 0) {
            toast("添加阅读条目成功")
        } else {
            toast("添加失败")
        }
    }

}