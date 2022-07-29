package com.jiaozhu.earphonereciver

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiaozhu.earphonereciver.Model.Bean
import com.jiaozhu.earphonereciver.Model.SharedModel
import com.jiaozhu.earphonereciver.Model.SharedModel.dao
import kotlinx.android.synthetic.main.activity_list.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var adapter: ListAdapter
    private lateinit var list: MutableList<Bean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        mRecyclerView.layoutManager = LinearLayoutManager(this) as RecyclerView.LayoutManager?
        list = dao.getHistory()
        adapter = ListAdapter(list)
        mRecyclerView.adapter = adapter
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initDrag()
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
                    dao.delete(model)
                } else {
                    model.isFinished = false
                    model.history = 0
                    dao.replace(model)
                    SharedModel.list.add(model)
                    dao.updateOrder(list)
                    adapter.notifyItemRemoved(position)
                }
                list.remove(model)
            }
        }).apply { attachToRecyclerView(mRecyclerView) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
