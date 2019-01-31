package com.jiaozhu.earphonereciver.comm

import android.content.Context
import android.content.SharedPreferences
import com.jiaozhu.earphonereciver.comm.PrefSupport.Companion.context
import com.jiaozhu.earphonereciver.comm.PrefSupport.Companion.default_rule
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by jiaozhu on 2017/6/27.
 */

class PrefSupport {
    companion object {
        lateinit var context: Context
        val default_rule = """推荐阅读[\s\S]*<=>null
//\n{1,}<==>\\n
//^[\s\S]*?关注\n<=>null
关闭无图模式[\s\S]*? 无图模式<==>null
著作权归作者所有[\s\S]*<==>null""".trimIndent()
    }
}


class Preferences<T>(val name: String, private val default: T)
    : ReadWriteProperty<Any?, T> {

    companion object {
        /**
         * 配置文件名称
         */
        val SHAREDPREFERENCES_NAME = "Setting"
        val prefs: SharedPreferences = context.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE)
        var rule by Preferences("rule", default_rule)//过滤规则
        var saveId: String? by Preferences("saveId", null)
        var savePoint: Int? by Preferences("savePoint", null)
    }


    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return findPreference(name, default)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        putPreference(name, value)
    }

    private fun <U> putPreference(name: String, value: U) = with(prefs.edit()) {
        when (value) {
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            is Int -> putInt(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            is Set<*> -> putStringSet(name, value as Set<String>)
            else -> throw IllegalArgumentException("This type can't be saved into Preferences ")
        }.apply()
    }

    @SuppressWarnings("unchecked")
    private fun <T> findPreference(name: String, default: T): T = with(prefs) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is String -> getString(name, default)
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            is Set<*> -> getStringSet(name, default as Set<String>)
            else -> throw IllegalArgumentException("This type can't be saved into Preferences")
        }
        res as T
    }
}
