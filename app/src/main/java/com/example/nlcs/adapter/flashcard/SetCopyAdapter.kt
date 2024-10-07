package com.example.nlcs.adapter.flashcard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nlcs.adapter.flashcard.SetCopyAdapter.SetViewHolder
import com.example.nlcs.data.dao.CardDAO
import com.example.nlcs.data.model.FlashCard
import com.example.nlcs.databinding.ItemSetCopyBinding
import com.example.nlcs.ui.activities.set.ViewSetActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SetCopyAdapter(private val context: Context, private val sets: ArrayList<FlashCard>) :
    RecyclerView.Adapter<SetViewHolder>() {
    var cardDAO: CardDAO? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ItemSetCopyBinding.inflate(inflater, parent, false)
        return SetViewHolder(binding.root)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        val set: FlashCard = sets[position]
        holder.binding.setNameTv.text = set.name

        val cardDAO = CardDAO(context)

        // Call the asynchronous function to count the cards
        cardDAO.countCardByFlashCardId(set.GetId()!!) { count ->
            // Update the UI with the number of terms after the query completes
            holder.binding.termCountTv.text = "$count thẻ "
        }

        holder.itemView.setOnClickListener { v: View? ->
            val intent = Intent(context, ViewSetActivity::class.java)
            intent.putExtra("id", set.GetId())
            context.startActivity(intent)
        }
    }


    override fun getItemCount(): Int {
        return sets.size
    }

    class SetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ItemSetCopyBinding = ItemSetCopyBinding.bind(itemView)
    }
}