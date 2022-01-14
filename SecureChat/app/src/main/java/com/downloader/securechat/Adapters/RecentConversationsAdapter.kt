package com.downloader.securechat.Adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.downloader.securechat.R
import com.downloader.securechat.databinding.ItemRecentConversationBinding
import com.downloader.securechat.models.RecentConversationChatMessage
import com.downloader.securechat.models.User

class RecentConversationsAdapter(recentMessage: ArrayList<RecentConversationChatMessage>, onConversationListener: onConversationListener): RecyclerView.Adapter<RecentConversationsAdapter.ConversationsViewHolder>() {

    private val recentChatMessage: ArrayList<RecentConversationChatMessage> = recentMessage
    private val conversationListener: onConversationListener = onConversationListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_conversation, parent, false)
        return ConversationsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationsViewHolder, position: Int) {
        val recentMessage = recentChatMessage[position]
        holder.binding.profilePic.setImageBitmap(getConversationImage(recentMessage.conversationImage))
        holder.binding.displayName.text = recentMessage.conversationName
        holder.binding.recentMessage.text = recentMessage.message
    }

    override fun getItemCount(): Int {
        return recentChatMessage.size
    }

    inner class ConversationsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val binding = ItemRecentConversationBinding.bind(itemView)
        init {
            binding.layout.setOnClickListener(this)
        }
        override fun onClick(v: View?) {
            //call the interface method to handle click on every item
            val user = User()
            user.id = recentChatMessage[adapterPosition].conversationId
            user.displayName = recentChatMessage[adapterPosition].conversationName
            user.encodedImage = recentChatMessage[adapterPosition].conversationImage
            conversationListener.onConversationClicked(user)   //get the user at the adapter position
        }
    }

    private fun getConversationImage(encodedProfilePic: String): Bitmap {
        val bytes = Base64.decode(encodedProfilePic, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    //handle clicks on each conversation from chatActivity
    interface onConversationListener {
        fun onConversationClicked(user: User)
    }

}