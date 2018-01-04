package com.jiaozhu.earphonereciver

import android.app.Application
import com.jiaozhu.ahibernate.util.DaoManager
import com.jiaozhu.earphonereciver.Model.DBHelper
import com.jiaozhu.earphonereciver.Model.Dao
import com.jiaozhu.earphonereciver.comm.PrefSupport

/**
 * Created by 教主 on 2017/12/18.
 */
public class CApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val dm = DaoManager.init(DBHelper(this))
        dm.registerDao(Dao::class.java)
        PrefSupport.context = this
    }
}