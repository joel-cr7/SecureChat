package com.downloader.securechat.Adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.downloader.securechat.R
import com.downloader.securechat.databinding.ItemReceivedMessageBinding
import com.downloader.securechat.databinding.ItemSentMessageBinding
import com.downloader.securechat.models.ChatMessage

class ChatAdapter(Messages: ArrayList<ChatMessage>, ProfilePic: Bitmap, senderID: String): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val chatMessages: ArrayList<ChatMessage> = Messages
    private val receiverProfilePic: Bitmap = ProfilePic
    private val senderId: String = senderID

    //variables to indicate whom the message is from
    companion object{
        val VIEW_TYPE_SENT = 1
        val VIEW_TYPE_RECEIVED = 2
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        //returning appropriate viewHolders according to the viewType

        //this is for sender ie. you
        if(viewType == VIEW_TYPE_SENT){
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sent_message, parent, false)
            return SentMessageViewHolder(view)
        }
        //this is for receivers messages ie. the person messaging you
        else{
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_received_message, parent, false)
            return ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //setting data according to 'viewType'
        if(getItemViewType(position) == VIEW_TYPE_SENT){
            val sentChatMessage = holder as SentMessageViewHolder   //get the holder as 'SentMessageViewHolder'
            sentChatMessage.sentMessageBinding.textMessage.text = chatMessages[position].message
            sentChatMessage.sentMessageBinding.textDateTime.text = chatMessages[position].dateTime
        }
        else{
            val receivedChatMessage = holder as ReceivedMessageViewHolder   //get the holder as 'ReceivedMessageViewHolder'
            receivedChatMessage.receivedMessageBinding.textMessage.text = chatMessages[position].message
            receivedChatMessage.receivedMessageBinding.textDateTime.text = chatMessages[position].dateTime
            receivedChatMessage.receivedMessageBinding.profilePic.setImageBitmap(receiverProfilePic)
        }
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }


    //this is to know from where message is come (sender or receiver). This will be used in 'onCreateViewHolder' as variable 'viewType' to set appropriate viewHolders
    override fun getItemViewType(position: Int): Int {
        if(chatMessages[position].senderId == senderId){
            return VIEW_TYPE_SENT
        }else{
            return VIEW_TYPE_RECEIVED
        }
    }



    //Here there are two viewHolders (for sender and receiver)
    inner class SentMessageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val sentMessageBinding = ItemSentMessageBinding.bind(itemView)
    }


    inner class ReceivedMessageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val receivedMessageBinding = ItemReceivedMessageBinding.bind(itemView)
    }

}