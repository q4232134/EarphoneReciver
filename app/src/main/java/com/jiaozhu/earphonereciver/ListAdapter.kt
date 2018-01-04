package com.jiaozhu.earphonereciver

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jiaozhu.earphonereciver.Model.Bean
import kotlinx.android.synthetic.main.item_content.view.*

/**
 * Created by 教主 on 2017/12/15.
 */
public class ListAdapter(private val list: List<Bean>) : RecyclerView.Adapter<ViewHolder>() {
    var onItemClickListener: OnItemClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_content, parent, false)).apply { itemView.mText.setOnClickListener { onItemClickListener?.onItemClick(itemView, adapterPosition) } }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = list[position]
        with(holder.itemView) {
            mText.text = model.title
            mText.isEnabled = !model.isFinished
            if (model.isPlaying) {
                mLayout.setBackgroundResource(R.drawable.bound)
            } else {
                mLayout.background = null
            }
        }
    }

    override fun getItemCount(): Int = list.size
}

class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

/**
 * 单击监听
 */
interface OnItemClickListener {
    fun onItemClick(view: View, position: Int)
}