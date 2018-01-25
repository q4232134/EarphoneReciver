package com.jiaozhu.earphonereciver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import com.jiaozhu.earphonereciver.Model.Bean
import com.jiaozhu.earphonereciver.Model.Dao
import com.jiaozhu.earphonereciver.comm.filtered
import getDao
import toast

/**
 * Created by 教主 on 2018/1/25.
 */
public class RecActivity : Activity() {
    companion object {
        private val dao = getDao(Dao::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }

    override fun onNewIntent(intent: Intent) {
        dealIntent(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        dealIntent(intent)
        finish()
    }

    /**
     * 处理请求
     */
    private fun dealIntent(intent: Intent) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.replace("\\n", "\n")
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