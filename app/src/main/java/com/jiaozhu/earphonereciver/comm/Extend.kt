import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jiaozhu.earphonereciver.Model.AppDatabase
import com.jiaozhu.earphonereciver.Model.Bean
import com.jiaozhu.earphonereciver.Model.BeanDao
import com.jiaozhu.earphonereciver.Model.SharedModel.list
import com.jiaozhu.earphonereciver.comm.filtered
import java.util.*


/**
 * 消息弹出扩展函数
 */
fun Context.toast(msg: Any?, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, msg.toString(), duration).show()
}

/**
 * 日志标签
 */
val Any.logTag: String?
    get() = this::class.simpleName

val recordTimeMap = Hashtable<String, Long>()
/**
 * 开始计时
 * @param tag 标签
 */
fun startTime(vararg tag: String = arrayOf("default")) {
    tag.forEach { recordTimeMap[it] = System.currentTimeMillis() }
}

/**
 * 结束计时
 * @param tag 标签
 */
fun stopTime(tag: String = "default"): Long? {
    if (!recordTimeMap.containsKey(tag)) return null
    val time = System.currentTimeMillis() - recordTimeMap[tag]!!
    recordTimeMap.remove("logTag")
    Log.i(tag, "$time")
    return time
}

/**
 * 随机获取列表中的元素
 */
fun <T> List<T>.randomTake(): T {
    return this[Random().nextInt(this.size)]
}


/**
 * 检测是否有权限，没有则申请
 */
fun Activity.checkPermission(requestCode: Int, vararg permissions: String, runnable: () -> Unit) {
    val needRequestList = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
    if (needRequestList.isEmpty()) {
        runnable()
    } else {
        ActivityCompat.requestPermissions(this, needRequestList.toTypedArray(), requestCode)
    }
}


/**
 * 处理请求
 */
fun Context.dealString(text: String?, dao: BeanDao): Boolean {
    if (text.isNullOrEmpty()) return false
    text.replace("\\n", "\n")
    val model = Bean(text.filtered + "\n下一条")
    if (list.contains(model)) {
        toast("条目已存在")
        return false
    }
    list.add(model)
    if (dao.replace(model) > 0) {
        toast("添加阅读条目成功")
        return true
    } else {
        toast("添加失败")
        return false
    }
}


