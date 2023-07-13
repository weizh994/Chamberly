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
        return if (message.uid == uid) {
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
    }

    /*private fun showPopupWindow(layoutInflater: LayoutInflater, anchorView: View) {
        val popupView = layoutInflater.inflate(R.menu.message_popup_menu, null)

        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)

        val xOff = (anchorView.width - popupView.measuredWidth) / 2
        val yOff = (anchorView.height - popupView.measuredHeight) / 2

        popupWindow.showAsDropDown(anchorView, xOff, yOff)
    }
    */

}


