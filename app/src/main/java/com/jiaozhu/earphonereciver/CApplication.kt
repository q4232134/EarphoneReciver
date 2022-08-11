package com.jiaozhu.earphonereciver

import android.app.Application
import androidx.room.Room
import com.jiaozhu.earphonereciver.model.AppDatabase
import com.jiaozhu.earphonereciver.model.SharedModel.dao
import com.jiaozhu.earphonereciver.comm.CrashHandler
import com.jiaozhu.earphonereciver.comm.PrefSupport
import com.jiaozhu.earphonereciver.model.SharedModel
import java.io.File


/**
 * Created by 教主 on 2017/12/18.
 */
class CApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "bean").allowMainThreadQueries().build()
        dao = db.dao()
        SharedModel.list = dao.getActiveBean()
        CrashHandler.init(this, getExternalFilesDir(null)?.path + File.separator + "crash.log")
        PrefSupport.context = this
        HttpService(8888).start()
    }
}