import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast
import com.jiaozhu.ahibernate.dao.impl.BaseDaoImpl
import com.jiaozhu.ahibernate.util.DaoManager
import java.util.*

/**
 * 消息弹出扩展函数
 */
fun Context.toast(msg: String?, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, msg, duration).show()
}

/**
 * 日志标签
 */
val Any.logTag: String?
    get() = this::class.simpleName

fun <T : BaseDaoImpl<*>> getDao(clazz: Class<T>): T = DaoManager.getInstance().getDao(clazz)

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

