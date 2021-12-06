package com.downloader.securechat.Adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.downloader.securechat.R
import com.downloader.securechat.databinding.ItemSingleContactBinding
import com.downloader.securechat.models.User

class ContactsAdapter(contactList: ArrayList<User>, onContactListener: onContactListener): RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder>() {

    private val contacts: ArrayList<User> = contactList
    private val contactListener: onContactListener = onContactListener


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_single_contact, parent, false)
        return ContactsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        val user = contacts[position]
        holder.binding.displayName.text = user.displayName
        holder.binding.displayNumber.text = user.phone_number
        Glide.with(holder.binding.profilePic.context).load(decodeProfilePic(user.encodedImage)).into(holder.binding.profilePic)
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    private fun decodeProfilePic(encodedImage: String): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val binding = ItemSingleContactBinding.bind(itemView)

        init {
            binding.profilePic.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            //calling the interface method to handle click on every item
            contactListener.onContactClicked(contacts[adapterPosition])   //get the user at the adapter position
        }
    }

    //to handle clicks on each contact
    interface onContactListener {
        fun onContactClicked(user: User)
    }

}