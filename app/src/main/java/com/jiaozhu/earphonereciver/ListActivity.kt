package com.jiaozhu.earphonereciver

import android.animation.ObjectAnimator
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import com.jiaozhu.earphonereciver.Model.Bean
import com.jiaozhu.earphonereciver.Model.Dao
import com.jiaozhu.earphonereciver.TTsService.Companion.list
import com.jiaozhu.earphonereciver.comm.PrefSupport
import com.jiaozhu.earphonereciver.comm.Preferences
import com.jiaozhu.earphonereciver.comm.filtered
import getDao
import kotlinx.android.synthetic.main.activity_list.*
import java.util.*


class ListActivity : AppCompatActivity(), OnItemClickListener, TTsService.Companion.TTSImpActivity {
    private lateinit var adapter: ListAdapter
    private var mItem: MenuItem? = null
    private var ttsService: TTsService? = null
    private var binder: TTsService.TTSBinder? = null
    private val dao = getDao(Dao::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        PrefSupport.context = this
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ListAdapter(list).apply { onItemClickListener = this@ListActivity }
        mRecyclerView.adapter = adapter
        initDrag()
        initService()
        mAdd.setOnClickListener {
            onAddClicked()
        }
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binder = service as TTsService.TTSBinder
            ttsService = binder!!.service
            ttsService?.callback = this@ListActivity
        }

        override fun onServiceDisconnected(name: ComponentName) {
        }

    }

    private fun initService() {
        bindService(Intent(this@ListActivity, TTsService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    /**
     * 刷新UI界面
     */
    private fun freshUI() {
        adapter.notifyDataSetChanged()
        if (mItem == null) return
        checkStatus()
    }

    override fun onStart() {
        super.onStart()
        freshUI()
        ttsService?.callback = this
    }

    override fun onStop() {
        super.onStop()
        ttsService?.callback = null
        list.forEachIndexed { index, it -> it.ord = index }
        dao.updateOrder(list)
    }

    private fun onAddClicked() {
        showEditDialog()
    }

    /**
     * 检查播放状态并设置播放按钮文字,是否为播放状态
     */
    private fun checkStatus() {
        if (binder == null) {
            mItem?.title = STR_PLAY
            return
        }
        if (binder!!.isPlaying) {
            mItem?.title = STR_STOP
        } else {
            mItem?.title = STR_PLAY
        }
    }

    /**
     * 初始化滑动组件
     */
    private fun initDrag() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.END or ItemTouchHelper.START) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val model = list[position]
                if (ItemTouchHelper.END == direction) {
                    dao.delete(model.code)
                    if (binder?.tag == model.code) binder?.stop()
                    list.removeAt(position)
                    adapter.notifyItemRemoved(position)
                } else {
                    model.isFinished = false
                    adapter.notifyItemChanged(position)
                }
            }

            override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
                adapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                Collections.swap(list, viewHolder.adapterPosition, target.adapterPosition)
            }
        }).apply { attachToRecyclerView(mRecyclerView) }
    }


    /**
     * 过滤规则对话框
     */
    private fun showSettingDialog() {
        val build = AlertDialog.Builder(this).setTitle("过滤规则(正则表达式)")
        val view = LayoutInflater.from(this).inflate(R.layout.view_content, null)
        val edit = view.findViewById<EditText>(R.id.mEdit).apply { hint = "在这里填写过滤规则" }
        build.setView(view)
        build.setPositiveButton("保存") { _, _ ->
            Preferences.rule = edit.text.toString()
        }.create().show()
        edit.setText(Preferences.rule)
    }

    /**
     *  弹出可编辑对话框
     */
    private fun showEditDialog(position: Int? = null) {
        var temp = position?.let { list[it] } ?: Bean("")
        val build = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.view_content, null)
        val edit = view.findViewById<EditText>(R.id.mEdit)
        build.setView(view)
        //修改按钮事件
        val onPositive = DialogInterface.OnClickListener { _, _ ->
            if (position == null) {
                temp = Bean(edit.text.toString().filtered + "\n下一条")
                dao.replace(temp)
                list.add(temp)
                adapter.notifyItemInserted(list.size - 1)
            } else {
                temp.content = edit.text.toString()
                temp.title = Bean.getHead(temp.content)
                dao.replace(temp)
                adapter.notifyItemChanged(position)
            }
        }
        val btnName = if (position == null) "添加" else "修改"
        build.setPositiveButton(btnName, onPositive).create().show()
        edit.setText(temp.content)

    }

    override fun onItemClick(view: View, position: Int) {
        showEditDialog(position)
    }


    override fun onItemChanged(position: Int) {
        checkStatus()
        if (position == -1) return
        adapter.notifyItemChanged(position)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }

    private fun pickUpAnimation(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationZ", 1f, 10f)
        animator.interpolator = DecelerateInterpolator()
        animator.duration = 300
        animator.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        mItem = menu.findItem(R.id.action_read)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_read -> {
                if (binder?.isPlaying == true) binder?.pause()
                else
                    binder?.start()
                return true
            }
            R.id.action_clear -> {
                val temps = list.filter { it.isFinished }
                dao.delete(temps.map { it.code })
                list.removeAll(temps)
                adapter.notifyDataSetChanged()
            }
            R.id.action_filter -> {
                showSettingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        val STR_PLAY = "播放"
        val STR_STOP = "暂停"
    }

    fun getMatchFromString(str: String) {
        val rules = str.split("\n")
    }



}
