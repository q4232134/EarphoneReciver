package com.jiaozhu.earphonereciver

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        val list = mutableListOf<String>()
        val s = """qweqw
            |
            |
            |qeqweqsda
            |asdadads
            |
            |
            |
            |
            |asd,sad,adasd,asd.afas.d
        """.trimMargin()
        s.split("\n", ",", "，", "。", "？", "?", "！", "!").filter { it.isNotEmpty()  }.forEach { list.add(it) }
        println(list)
    }
}