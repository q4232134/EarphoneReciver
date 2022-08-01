package com.jiaozhu.earphonereciver.model

object SharedModel {
    lateinit var list: MutableList<Bean>
    lateinit var dao: BeanDao
    var currentTag: String? = null

}