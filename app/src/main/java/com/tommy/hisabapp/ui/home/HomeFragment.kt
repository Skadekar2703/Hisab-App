package com.tommy.hisabapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.tommy.hisabapp.MainActivity
import com.tommy.hisabapp.R
import com.tommy.hisabapp.data.model.TransactionType
import com.tommy.hisabapp.databinding.FragmentHomeBinding
import com.tommy.hisabapp.ui.MainViewModel
import com.tommy.hisabapp.ui.TransactionListAdapter
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels {
        (requireActivity() as MainActivity).viewModelFactory
    }
    private lateinit var adapter: TransactionListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TransactionListAdapter(viewModel, showArrows = true, onLongPress = ::confirmDelete)
        binding.transactionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionsRecycler.adapter = adapter
        binding.avatarInitials.text = (requireActivity() as MainActivity).currentUserInitial()
        binding.menuIcon.setOnClickListener { (requireActivity() as MainActivity).showLogoutPrompt() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val recent = state.transactions.take(4)
                    val income = state.transactions
                        .filter { it.type == TransactionType.DEPOSIT || it.type == TransactionType.TRANSFER_IN }
                        .sumOf { it.amount }
                    val expense = state.transactions
                        .filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.TRANSFER_OUT }
                        .sumOf { it.amount }

                    binding.balanceAmount.text = viewModel.formatCurrency(state.balance)
                    binding.incomeAmount.text = "+${viewModel.formatCurrencyNoDecimals(income)}"
                    binding.expenseAmount.text = "-${viewModel.formatCurrencyNoDecimals(expense)}"
                    adapter.submitList(recent)
                    binding.emptyStateText.isVisible = recent.isEmpty()
                }
            }
        }
    }

    private fun confirmDelete(item: com.tommy.hisabapp.data.model.TransactionItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_transaction)
            .setMessage(R.string.delete_transaction_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteTransaction(item.id) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
