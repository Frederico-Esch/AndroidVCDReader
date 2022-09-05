package com.frederico.vcd

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterInfo(val context: Context, val scopes : ArrayList<String>, val onClick : (pos:Int) -> Unit) : RecyclerView.Adapter<AdapterInfo.ViewHolder>() {

    class ViewHolder(itemView: View, val onClick: (pos:Int) -> Unit) : RecyclerView.ViewHolder(itemView) {
        val title : TextView
        init {
            title = itemView.findViewById(R.id.title_scope)
            itemView.setOnClickListener {
                if(adapterPosition >= 0) onClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.signal_item, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = scopes[position]
    }

    override fun getItemCount(): Int {
        return scopes.size
    }
}