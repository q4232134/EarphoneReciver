package com.jiaozhu.earphonereciver.comm

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by jiaozhu on 16/3/16.
 * 错误日志记录
 */
class CrashHandler private constructor(private val context: Context, private val logPath: String) : Thread.UncaughtExceptionHandler {
    private val format = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

    /**
     * 获取手机信息
     *
     * @return
     */
    private//版本号
    //手机制造商
    val phoneInfo: StringBuffer
        get() {
            val pm = context.packageManager
            val pi: PackageInfo
            val sb = StringBuffer()
            try {
                pi = pm.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
                sb.append("AppName:").append(context.packageName).append("App Version:")
                        .append(pi.versionName).append("_").append(pi.versionCode).append("\n")
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

            sb.append("OS Version:").append(Build.VERSION.RELEASE).append("_")
                    .append(Build.VERSION.SDK_INT).append("\n")
            sb.append("Vendor:").append(Build.MANUFACTURER).append("\nModel").append(Build.MODEL)
                    .append("\nCPU ABI:").append(Build.CPU_ABI).append("\n")
            return sb
        }


    override fun uncaughtException(thread: Thread, ex: Throwable) {
        val content = StringBuffer()
        content.append(phoneInfo).append(ex.toString()).append("\n")
        for (element in ex.stackTrace) {
            content.append(element).append("\n")
        }
        content.append(format.format(Date()) + "---------------------------------------------------------------\n")
        writeFile(File(logPath), content.toString())
        if (defaultHandler == null) {
            android.os.Process.killProcess(android.os.Process.myPid())
        } else {
            defaultHandler!!.uncaughtException(thread, ex)
        }

    }

    companion object {
        private var defaultHandler: Thread.UncaughtExceptionHandler? = null
        private var crashHandler: CrashHandler? = null

        fun init(context: Context, logPath: String) {
            if (crashHandler == null) {
                crashHandler = CrashHandler(context, logPath)
            }
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        }

        /**
         * 写入文件
         *
         * @param file 需要写入的文件
         * @param str  需要写入的内容
         * @return 写入是否成功
         */
        private fun writeFile(file: File, str: String): Boolean {
            var flag = false
            try {
                if (!file.exists()) {
                    file.createNewFile()
                }
                val fos = FileOutputStream(file, true)
                val bytes = str.toByteArray()
                fos.write(bytes)
                fos.close()
                flag = true
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return flag
        }
    }
}
