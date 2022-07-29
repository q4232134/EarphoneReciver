package com.jiaozhu.earphonereciver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jiaozhu.earphonereciver.Model.Bean
import kotlinx.android.synthetic.main.item_content.view.*
import java.text.SimpleDateFormat

/**
 * Created by 教主 on 2017/12/15.
 */
public class ListAdapter(private val list: List<Bean>) : RecyclerView.Adapter<ViewHolder>() {
    var onItemClickListener: OnItemClickListener? = null
    private val format = SimpleDateFormat("yyyy-MM-dd")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_content, parent, false)).apply { itemView.mText.setOnClickListener { onItemClickListener?.onItemClick(itemView, adapterPosition) } }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = list[position]
        with(holder.itemView) {
            mText.text = model.title
            mText.isEnabled = !model.isFinished
            mTime.text = format.format(model.createTime)
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