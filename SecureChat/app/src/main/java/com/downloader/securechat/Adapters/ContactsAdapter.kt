package com.downloader.securechat.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.downloader.securechat.R
import com.downloader.securechat.databinding.ItemSingleContactBinding
import com.downloader.securechat.models.User

class ContactsAdapter(contactList: ArrayList<User>): RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder>() {

    private val contacts: ArrayList<User> = contactList


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_single_contact, parent, false)
        return ContactsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        val user = contacts[position]
        holder.binding.displayName.text = user.displayName
        holder.binding.displayNumber.text = user.email
        Glide.with(holder.binding.profilePic.context).load(user.imageUrl).into(holder.binding.profilePic)
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemSingleContactBinding.bind(itemView)
    }

}