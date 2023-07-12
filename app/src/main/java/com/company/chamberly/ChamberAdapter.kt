package com.company.chamberly

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class ChamberAdapter(private val context: Context) : BaseAdapter() {

    val dataList = mutableListOf<Chamber>()

    override fun getCount(): Int {
        return dataList.size
    }

    override fun getItem(position: Int): Chamber {
        return dataList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setData(chamber: Chamber) {
        //dataList.clear() // 清空列表，只保留最新的一个 Chamber 对象
        dataList.add(chamber)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_koloda, parent, false)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val chamber = getItem(position)
        textTitle.text = "\"${chamber.groupTitle}\""
        return view
    }
}
