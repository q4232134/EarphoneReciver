package com.jiaozhu.earphonereciver

import android.app.Application
import androidx.room.Room
import com.jiaozhu.earphonereciver.Model.AppDatabase
import com.jiaozhu.earphonereciver.Model.Support
import com.jiaozhu.earphonereciver.comm.PrefSupport


/**
 * Created by 教主 on 2017/12/18.
 */
public class CApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Support.db = Room.databaseBuilder(this, AppDatabase::class.java, "bean").allowMainThreadQueries().build()
        PrefSupport.context = this
    }
}