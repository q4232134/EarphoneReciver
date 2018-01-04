package com.jiaozhu.earphonereciver.comm

import android.annotation.SuppressLint
import android.support.v7.app.ActionBar
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import java.util.*

/**
 * Created by jiaozhu on 16/3/22.
 */
abstract class SelectorRecyclerAdapter<T : RecyclerView.ViewHolder> : RecyclerView.Adapter<T>() {
    protected var isSelectModel = false
    /**
     * 获取选中列表
     *
     * @return
     */
    var selectList = HashSet<Int>()
        protected set//被选中条目列表
    var selectorListener: SelectorStatusChangedListener? = null
    var itemListener: ItemStatusChangedListener? = null
    var itemClickListener: OnItemClickListener? = null
    private var lastSelectedItem = 0//上一个被选中的item(用于单选模式)
    var selectorMode = 0

    private var actionView: ActionBar? = null//顶部工具栏
    private var actionItemClickedListener: ActionItemClickedListener? = null//菜单单击监听器
    private var actionMode: ActionMode? = null

    /**
     * 设置actionMode
     *
     * @param actionView                toolbar或者actionbar
     * @param actionItemClickedListener 回调监听
     */
    fun setActionView(actionView: ActionBar, actionItemClickedListener: ActionItemClickedListener) {
        this.actionView = actionView
        this.actionItemClickedListener = actionItemClickedListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T {
        val vh = onCreateHolder(parent, viewType)
        vh.itemView.setOnClickListener { v ->
            val position = vh.layoutPosition
            if (isSelectModel) {
                if (isSelect(position)) {
                    removeSelect(position)
                } else {
                    setSelect(position)
                }
            } else {
                onItemClick(position, v)
            }
        }
        if (selectorMode != MODE_NONE)
            vh.itemView.setOnLongClickListener(View.OnLongClickListener {
                val position = vh.layoutPosition
                if (itemSelectable(position)) {
                    startSelectorMode()
                    setSelect(position)
                    return@OnLongClickListener true
                }
                false
            })
        return vh
    }

    override fun onBindViewHolder(holder: T, position: Int) {
        onBindView(holder, position, isSelectModel && isSelect(position))
    }

    /**
     * 启动选择模式
     */
    private fun startSelectorMode() {
        if (!isSelectModel && selectorMode != MODE_NONE) {
            isSelectModel = true
            selectList.clear()//初始化
            if (selectorListener != null) {
                selectorListener!!.onSelectorStatusChanged(true)
            }
            startActionMode()
        }
    }

    /**
     * 启动action模式
     */
    @SuppressLint("RestrictedApi")
    private fun startActionMode() {
        if (actionView == null || actionItemClickedListener == null) return
        actionMode = actionView!!.startActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                return actionItemClickedListener!!.onCreateActionMode(mode, menu)
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val flag = actionItemClickedListener!!.onActionItemClicked(mode, item)
                if (flag) cancelSelectorMode()
                return flag
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                cancelSelectorMode()
            }
        })
    }

    /**
     * 取消选择模式
     */
    fun cancelSelectorMode() {
        if (isSelectModel) {
            isSelectModel = false
            selectList.clear()
            this.notifyDataSetChanged()
            if (selectorListener != null) {
                selectorListener!!.onSelectorStatusChanged(false)
            }
            if (actionView != null && actionItemClickedListener != null) {
                actionMode!!.finish()
            }
        }
    }

    /**
     * 选中条目
     *
     * @param position
     * @return 选中是否成功
     */
    fun setSelect(position: Int): Boolean {
        if (!itemSelectable(position)) return false
        selectList.add(position)
        if (selectorMode == MODE_SINGER) {
            removeSelect(lastSelectedItem)
            lastSelectedItem = position
        }
        if (itemListener != null)
            itemListener!!.onItemStatusChanged(position, true)
        notifyItemChanged(position)
        return true
    }

    /**
     * 取消条目
     *
     * @param position
     */
    fun removeSelect(position: Int) {
        selectList.remove(position)
        if (itemListener != null)
            itemListener!!.onItemStatusChanged(position, false)
        notifyItemChanged(position)
        if (selectList.isEmpty()) {
            cancelSelectorMode()
        }
    }

    /**
     * 检查条目是否被选中
     *
     * @param position
     * @return
     */
    fun isSelect(position: Int): Boolean {
        return selectList.contains(position)
    }


    /**
     * 重写此方法决定item是否能够被选中
     *
     * @param position
     * @return
     */
    protected fun itemSelectable(position: Int): Boolean {
        return true
    }


    /**
     * 参考 T onCreateViewHolder(ViewGroup parent, int viewType)
     */
    protected abstract fun onCreateHolder(parent: ViewGroup, viewType: Int): T

    /**
     * 参考 void onBindViewHolder(final T holder, int position)
     *
     * @param isSelected 是否被选中
     */
    abstract fun onBindView(holder: T, position: Int, isSelected: Boolean)

    /**
     * 通过重写此方法添加单击监听
     *
     * @param position
     * @param view
     */
    protected fun onItemClick(position: Int, view: View) {
        if (itemClickListener != null)
            itemClickListener!!.onItemClick(view, position)
    }

    interface SelectorStatusChangedListener {
        /**
         * 选择器状态改变时调用
         *
         * @param isSelectedMod 是否为选择器状态
         */
        fun onSelectorStatusChanged(isSelectedMod: Boolean)
    }

    interface ItemStatusChangedListener {
        /**
         * 条目状态改变时调用
         *
         * @param isSelected 是否为被选中状态
         */
        fun onItemStatusChanged(position: Int, isSelected: Boolean)
    }

    interface ActionItemClickedListener {
        /**
         * 创建action模式菜单
         *
         * @param menu
         * @return
         */
        fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean

        /**
         * action单击监听
         *
         * @param item
         * @return
         */
        fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean

    }

    /**
     * 单击监听
     */
    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    companion object {
        val MODE_NONE = 0//普通模式
        val MODE_SINGER = 1//单选模式
        val MODE_MULTI = 2//多选模式
    }

}
