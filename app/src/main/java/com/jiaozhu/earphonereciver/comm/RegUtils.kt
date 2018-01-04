package com.jiaozhu.earphonereciver.comm

import java.util.regex.Pattern

/**
 * Created by 教主 on 2017/12/29.
 */
object RegUtils {
    data class Result(val old: String, val new: String, val mathAll: Boolean)


    fun filter(msg: String, rule: String): String {
        val list = rule.split("\n").filter { !it.startsWith("""//""") }.map {
            val temp = it.split("<==>", "<=>")
            var new = if (temp[1].equals("null", true)) "" else temp[1]
            Result(temp[0], new, it.contains("<==>"))
        }
        val matchs = list.map { Pattern.compile(it.old) }
        var temp = msg
        matchs.forEachIndexed { index, it ->
            val ma = it.matcher(temp)
            val rs = list[index]
            temp = if (rs.mathAll) ma.replaceAll(rs.new) else ma.replaceFirst(rs.new)
        }
        return temp
    }
}

public val String?.filtered: String?
    get() {
        if (this == null) return null
        return RegUtils.filter(this, Preferences.rule)
    }