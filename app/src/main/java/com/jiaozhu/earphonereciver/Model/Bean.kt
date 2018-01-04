package com.jiaozhu.earphonereciver.Model

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import com.jiaozhu.ahibernate.annotation.Column
import com.jiaozhu.ahibernate.annotation.Id
import com.jiaozhu.ahibernate.annotation.Table
import com.jiaozhu.ahibernate.dao.impl.BaseDaoImpl
import com.jiaozhu.ahibernate.util.MyDBHelper
import kotlin.math.min


/**
 * Created by 教主 on 2017/12/18.
 */
class DBHelper(context: Context) : MyDBHelper(context, "Bean", null, 1) {
    fun onUpgrade() {
    }
}

class Dao(dbHelper: SQLiteOpenHelper) : BaseDaoImpl<Bean>(dbHelper) {
    /**
     * 获取未完成列表
     */
    fun getActiveBean(): MutableList<Bean> = rawQuery("select * from $tableName order by ord", null)

    /**
     * 更新列表
     */
    fun updateOrder(lists: List<Bean>) {
        update(lists, setOf("ord", "isFinished"))
    }
}

@Table
data class Bean(@Id @Column var code: String = "",
                @Column var title: String = "",
                @Column var content: String = "",
                @Column var ord: Int = 0,
                @Column var isFinished: Boolean = false,
                var isPlaying: Boolean = false) {

    constructor(msg: String) : this(
            System.currentTimeMillis().toString(),
            getHead(msg),
            msg, -1,
            false)


    override fun equals(other: Any?): Boolean {
        val obj = other as? Bean ?: return false
        return content.length == obj.content.length && title == obj.title
    }

    companion object {
        fun getHead(msg: String) = msg.subSequence(0..min(msg.length - 1, 100)).toString().replace("\n", " ")
    }
}