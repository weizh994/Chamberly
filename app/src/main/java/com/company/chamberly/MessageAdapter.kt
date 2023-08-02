package com.company.chamberly

import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val uid: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val messages: MutableList<Message> = mutableListOf()
    private var onMessageLongClickListener: OnMessageLongClickListener? = null

    fun setOnMessageLongClickListener(listener: OnMessageLongClickListener) {
        onMessageLongClickListener = listener
    }

    companion object {
        private const val VIEW_TYPE_ME = 1
        private const val VIEW_TYPE_OTHER = 2
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textSender: TextView = itemView.findViewById(R.id.text_gchat_user_other)
        val textMessage: TextView = itemView.findViewById(R.id.text_gchat_message_other)
        init {
            // set on long click listener
            itemView.setOnLongClickListener {
                val message = messages[adapterPosition]
                onMessageLongClickListener!!.onMessageLongClick(message)
                onMessageLongClickListener!!.onSelfLongClick(message)
                true
            }
        }
    }

    inner class MessageViewHolderMe(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMessage: TextView = itemView.findViewById(R.id.text_gchat_message_me)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ME -> {
                val itemView = inflater.inflate(R.layout.item_chat_me, parent, false)
                MessageViewHolderMe(itemView)
            }
            VIEW_TYPE_OTHER -> {
                val itemView = inflater.inflate(R.layout.item_chat_other, parent, false)
                MessageViewHolder(itemView)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is MessageViewHolderMe -> {
                holder.textMessage.text = message.message_content
                holder.itemView.setOnLongClickListener {
                    onMessageLongClickListener?.onSelfLongClick(message)
                    true
                }
            }
            is MessageViewHolder -> {
                holder.textSender.text = message.sender_name
                holder.textMessage.text = message.message_content
                holder.itemView.setOnLongClickListener {
                    onMessageLongClickListener?.onMessageLongClick(message)
                    true
                }
            }
        }
    }

    fun setMessages(messages: List<Message>) {
        this.messages.clear()
        this.messages.addAll(messages)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.UID == uid) {
            VIEW_TYPE_ME
        } else {
            VIEW_TYPE_OTHER
        }
    }

    fun addMessage(message: Message) {
        messages.add(message)
        notifyDataSetChanged()
    }


    interface OnMessageLongClickListener {
        fun onMessageLongClick(message: Message)
        fun onSelfLongClick(message: Message)
    }


}


