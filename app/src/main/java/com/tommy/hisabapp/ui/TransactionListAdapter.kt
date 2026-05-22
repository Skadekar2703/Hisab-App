package com.tommy.hisabapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tommy.hisabapp.R
import com.tommy.hisabapp.data.model.TransactionItem
import com.tommy.hisabapp.data.model.TransactionType
import com.tommy.hisabapp.databinding.ItemTransactionBinding

class TransactionListAdapter(
    private val viewModel: MainViewModel,
    private val showArrows: Boolean = false,
    private val onLongPress: (TransactionItem) -> Unit
) : ListAdapter<TransactionItem, TransactionListAdapter.TransactionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return TransactionViewHolder(ItemTransactionBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionItem) {
            val style = TransactionVisuals.styleFor(item)
            binding.iconBadge.setBackgroundResource(style.badgeBackgroundRes)
            binding.iconImage.setImageResource(style.iconRes)
            binding.categoryText.text = item.note.ifBlank { item.category.ifBlank { item.type.displayName() } }
            binding.dateText.text = viewModel.formatDate(item.date)
            binding.chipText.text = item.category.ifBlank { item.type.displayName() }
            binding.chipText.setBackgroundResource(style.chipBackgroundRes)
            binding.chipText.setTextColor(
                ContextCompat.getColor(binding.root.context, style.chipTextColorRes)
            )

            var signedAmount = when (item.type) {
                TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> "-${viewModel.formatCurrencyNoDecimals(item.amount)}"
                TransactionType.DEPOSIT, TransactionType.TRANSFER_IN -> "+${viewModel.formatCurrencyNoDecimals(item.amount)}"
            }
            if (showArrows) {
                signedAmount += when (item.type) {
                    TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> " ↓"
                    TransactionType.DEPOSIT, TransactionType.TRANSFER_IN -> " ↑"
                }
            }
            binding.amountText.text = signedAmount
            val colorRes = when (item.type) {
                TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> R.color.hisab_expense
                TransactionType.DEPOSIT, TransactionType.TRANSFER_IN -> R.color.hisab_income
            }
            binding.amountText.setTextColor(ContextCompat.getColor(binding.root.context, colorRes))
            binding.typeText.text = item.type.displayName()
            binding.root.setOnLongClickListener {
                onLongPress(item)
                true
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TransactionItem>() {
            override fun areItemsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean =
                oldItem == newItem
        }
    }
}
