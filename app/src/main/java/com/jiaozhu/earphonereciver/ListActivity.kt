package com.jiaozhu.earphonereciver

import android.content.*
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiaozhu.earphonereciver.model.Bean
import com.jiaozhu.earphonereciver.comm.PrefSupport.Companion.context
import com.jiaozhu.earphonereciver.comm.Preferences
import com.jiaozhu.earphonereciver.comm.filtered
import com.jiaozhu.earphonereciver.model.SharedModel.dao
import com.jiaozhu.earphonereciver.model.SharedModel.list
import dealString
import kotlinx.android.synthetic.main.activity_list.*
import java.lang.reflect.Method
import java.util.*


class ListActivity : AppCompatActivity(), OnItemClickListener {
    private lateinit var adapter: ListAdapter
    private lateinit var clipboard: ClipboardManager
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mItem: MenuItem? = null

    inner class CLayoutManager(context: Context) : LinearLayoutManager(context) {
        override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
            try {
                super.onLayoutChildren(recycler, state)
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }
    }

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaBrowser.sessionToken.also { token ->
                val controller = MediaControllerCompat(this@ListActivity, token)
                MediaControllerCompat.setMediaController(this@ListActivity, controller)
                controller.registerCallback(mediaCallback)
            }
            MediaControllerCompat.getMediaController(this@ListActivity).registerCallback(mediaCallback)
        }
    }


    var lastState: Int? = 0
    val mediaCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            checkStatus()
        }

        override fun onPlaybackStateChanged(@Nullable state: PlaybackStateCompat?) {
            if (state == null || lastState == state?.state) return
            lastState = state.state
            val tag = state.extras!!.getString("tag")
            val index = list.indexOfFirst { it.id == tag }
            when (lastState) {
                PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.STATE_PAUSED -> onItemChanged(index)
                PlaybackStateCompat.STATE_NONE -> onItemRemoved(index)
                else -> {}
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        context = this
        mRecyclerView.layoutManager = CLayoutManager(this)
        list = dao.getActiveBean()
        adapter = ListAdapter(list).apply { onItemClickListener = this@ListActivity }
        mRecyclerView.adapter = adapter
        initClipboard()
        initDrag()
        initService()
        mAdd.setOnClickListener {
            onAddClicked()
        }
        mAdd.setOnLongClickListener {
            onAddLongClicked()
        }
        mediaBrowser = MediaBrowserCompat(this, ComponentName(this, MediaPlaybackService::class.java), connectionCallbacks, null)
    }

    private fun onAddLongClicked(): Boolean {
        val temp = (clipboard.primaryClip?.getItemAt(0)?.text ?: "").toString()
        dealString(temp, dao)
        return true
    }


    /**
     * 初始化剪切板监听
     */
    var text = ""

    private fun initClipboard() {
        clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener {
            val temp = (clipboard.primaryClip?.getItemAt(0)?.text ?: "").toString()
            if (dealString(temp, dao)) return@addPrimaryClipChangedListener
        }
    }


    private fun initService() {
        bindService(
            Intent(this@ListActivity, MediaPlaybackService::class.java),
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    println("onServiceConnected")
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    println("onServiceDisconnected")
                }
            },
            Context.BIND_AUTO_CREATE
        )
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
        mediaBrowser.connect();
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }


    override fun onStop() {
        super.onStop()
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(mediaCallback)
        mediaBrowser.disconnect()
    }

    private fun onAddClicked() {
        showEditDialog()
    }

    /**
     * 检查播放状态并设置播放按钮文字,是否为播放状态
     */
    private fun checkStatus() {
        mItem?.title = when (lastState) {
            PlaybackStateCompat.STATE_PLAYING -> STR_STOP
            PlaybackStateCompat.STATE_STOPPED -> STR_PLAY
            PlaybackStateCompat.STATE_PAUSED -> STR_PLAY
            else -> STR_PLAY
        }
    }

    /**
     * 初始化滑动组件
     */
    private fun initDrag() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.END or ItemTouchHelper.START
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val model = list[position]
                if (ItemTouchHelper.END == direction) {
                    dao.delete(model)
                } else {
                    model.isFinished = true
                    dao.replace(model)
                }
                adapter.notifyItemRemoved(position)
                list.removeAt(position)
            }

            override fun onMoved(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                fromPos: Int,
                target: RecyclerView.ViewHolder,
                toPos: Int,
                x: Int,
                y: Int
            ) {
                adapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                Collections.swap(list, viewHolder.adapterPosition, target.adapterPosition)
                list.forEachIndexed { index, it -> it.ord = index }
                dao.updateOrder(list)
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
        try {
            val debugDB = Class.forName("com.amitshekhar.DebugDB")
            val getAddressLog: Method = debugDB.getMethod("getAddressLog")
            val value: Any? = getAddressLog.invoke(null)
            edit.hint = "复制需要朗读的文本到这里或者\n使用网页打开${
                (value as String).replace("8080", "8888").replace("Open", "")
                    .replace(" in your browser", "")
            }"
        } catch (ignore: Exception) {
        }
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


    fun onItemChanged(position: Int) {
        checkStatus()
        if (position == -1) return
        adapter.notifyItemChanged(position)
    }

    fun onItemRemoved(position: Int) {
        checkStatus()
        if (position == -1) return
        adapter.notifyItemRemoved(position)
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        mItem = menu.findItem(R.id.action_read)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_read -> {
                val controller = MediaControllerCompat.getMediaController(this)
                val pbState = controller.playbackState.state
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
                return true
            }
            R.id.action_history -> {
                val i = Intent(this, HistoryActivity::class.java)
                startActivity(i)
            }
            R.id.action_filter -> {
                showSettingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val STR_PLAY = "播放"
        const val STR_STOP = "暂停"
    }


}
