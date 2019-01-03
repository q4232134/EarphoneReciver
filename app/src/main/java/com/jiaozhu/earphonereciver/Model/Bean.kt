package com.jiaozhu.earphonereciver.Model

import androidx.lifecycle.ViewModel
import androidx.room.*
import java.util.*
import kotlin.math.min


/**
 * Created by 教主 on 2017/12/18.
 */
@Database(entities = [Bean::class], version = 1)
@TypeConverters(value = [Converters::class])
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): BeanDao
}

interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun replace(t: T): Long


    @Delete
    fun delete(vararg beans: T)


    @Delete
    fun delete(beans: List<T>)

}

@Dao
interface BeanDao : BaseDao<Bean> {
    /**
     * 获取未完成列表
     */
    @Query("select * from Bean order by ord")
    fun getActiveBean(): MutableList<Bean>

    /**
     * 更新列表
     */
    @Update
    fun updateOrder(beans: List<Bean>)

}

@Entity(tableName = "Bean")
data class Bean(@PrimaryKey @ColumnInfo(name = "code") var id: String = "",
                var title: String = "",
                var content: String = "",
                var ord: Int = 0,
                var isFinished: Boolean = false,
                var createTime: Date = Date(),
                var history: Int = 0,
                @Ignore var isPlaying: Boolean = false) : ViewModel() {

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


class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

