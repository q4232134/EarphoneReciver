package com.jiaozhu.earphonereciver

import android.app.Application
import android.util.Log
import android.util.Log.d
import androidx.room.Room
import com.jiaozhu.earphonereciver.model.AppDatabase
import com.jiaozhu.earphonereciver.model.SharedModel.dao
import com.jiaozhu.earphonereciver.comm.CrashHandler
import com.jiaozhu.earphonereciver.comm.PrefSupport
import com.jiaozhu.earphonereciver.model.SharedModel
import logTag
import java.io.File
import java.util.concurrent.Executors
import java.util.logging.Logger


/**
 * Created by 教主 on 2017/12/18.
 */
class CApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val dbu = Room.databaseBuilder(this, AppDatabase::class.java, "bean")
        dbu.setQueryCallback({ sqlQuery, bindArgs ->
            d(logTag,"SQL Query: $sqlQuery \nSQL Args: $bindArgs")
        }, Executors.newSingleThreadExecutor())
        val db = dbu.allowMainThreadQueries().build()
        dao = db.dao()
        SharedModel.list = dao.getActiveBean()
        CrashHandler.init(this, getExternalFilesDir(null)?.path + File.separator + "crash.log")
        PrefSupport.context = this
        HttpService(8888).start()
    }
}