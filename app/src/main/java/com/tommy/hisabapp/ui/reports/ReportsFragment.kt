package com.tommy.hisabapp.ui.reports

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.tommy.hisabapp.MainActivity
import com.tommy.hisabapp.R
import com.tommy.hisabapp.databinding.FragmentReportsBinding
import com.tommy.hisabapp.ui.MainViewModel
import com.tommy.hisabapp.ui.TransactionListAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsFragment : Fragment() {
    private var _binding: FragmentReportsBinding? = null
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
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TransactionListAdapter(viewModel, showArrows = false, onLongPress = ::confirmDelete)
        binding.reportRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.reportRecycler.adapter = adapter
        binding.avatarInitials.text = (requireActivity() as MainActivity).currentUserInitial()
        binding.menuIcon.setOnClickListener { (requireActivity() as MainActivity).showLogoutPrompt() }
        binding.filterButton.setOnClickListener { showFilterDialog() }

        renderMonthChips()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.reportDateText.isVisible = state.reportByDay
                    binding.reportDateText.text = state.reportLabel
                    adapter.submitList(state.reportTransactions)
                    binding.emptyReportText.isVisible = state.reportTransactions.isEmpty()
                    renderChart(state.reportPieData)
                    renderLegend(state.reportPieData)
                }
            }
        }
    }

    private fun renderMonthChips() {
        binding.monthChipContainer.removeAllViews()
        val current = Calendar.getInstance().apply { timeInMillis = viewModel.uiState.value.selectedReportTime }

        repeat(4) { index ->
            val chipDate = Calendar.getInstance().apply {
                timeInMillis = current.timeInMillis
                add(Calendar.MONTH, -index)
            }
            val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(chipDate.timeInMillis))
            val selected = !viewModel.uiState.value.reportByDay &&
                chipDate.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
                chipDate.get(Calendar.MONTH) == current.get(Calendar.MONTH)
            val chip = com.google.android.material.button.MaterialButton(requireContext()).apply {
                text = label
                textSize = 12f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (selected) R.color.hisab_brown_dim else R.color.hisab_text_secondary
                    )
                )
                backgroundTintList = ContextCompat.getColorStateList(
                    context,
                    if (selected) R.color.hisab_orange else R.color.hisab_surface_mid
                )
                insetTop = 0
                insetBottom = 0
                cornerRadius = 999
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    resources.getDimensionPixelSize(R.dimen.month_chip_height)
                ).also { params ->
                    if (index > 0) params.marginStart = resources.getDimensionPixelSize(R.dimen.spacing_md)
                }
                setOnClickListener {
                    viewModel.setReportMode(false)
                    viewModel.setReportTime(chipDate.timeInMillis)
                    renderMonthChips()
                }
            }
            binding.monthChipContainer.addView(chip)
        }

        val calendarChip = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "\uD83D\uDCC5"
            textSize = 12f
            backgroundTintList = ContextCompat.getColorStateList(
                context,
                if (viewModel.uiState.value.reportByDay) R.color.hisab_orange else R.color.hisab_surface_mid
            )
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (viewModel.uiState.value.reportByDay) R.color.hisab_brown_dim else R.color.hisab_text_secondary
                )
            )
            insetTop = 0
            insetBottom = 0
            cornerRadius = 999
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(R.dimen.month_chip_height)
            ).also { params ->
                params.marginStart = resources.getDimensionPixelSize(R.dimen.spacing_md)
            }
            setOnClickListener { showDatePicker() }
        }
        binding.monthChipContainer.addView(calendarChip)
    }

    private fun showFilterDialog() {
        val items = arrayOf(getString(R.string.filter_month), getString(R.string.filter_day))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.filter_mode_title)
            .setItems(items) { _, which ->
                viewModel.setReportMode(which == 1)
                if (which == 1) {
                    showDatePicker()
                } else {
                    renderMonthChips()
                }
            }
            .show()
    }

    private fun showDatePicker() {
        val current = Calendar.getInstance().apply { timeInMillis = viewModel.uiState.value.selectedReportTime }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                current.set(year, month, day, 0, 0, 0)
                current.set(Calendar.MILLISECOND, 0)
                viewModel.setReportTime(current.timeInMillis)
                renderMonthChips()
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun renderChart(data: Map<String, Double>) {
        val chart = binding.pieChart
        if (data.isEmpty()) {
            chart.clear()
            val totalFormatted = viewModel.formatCurrencyNoDecimals(0.0)
            val centerTextStr = "$totalFormatted\n${getString(R.string.spent)}"
            val spannable = android.text.SpannableString(centerTextStr).apply {
                setSpan(android.text.style.RelativeSizeSpan(1.6f), 0, totalFormatted.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, totalFormatted.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.hisab_text_primary)), 0, totalFormatted.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                
                val startSpent = totalFormatted.length + 1
                val endSpent = centerTextStr.length
                setSpan(android.text.style.RelativeSizeSpan(0.85f), startSpent, endSpent, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.hisab_text_secondary)), startSpent, endSpent, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            chart.centerText = spannable
            chart.invalidate()
            return
        }

        val palette = listOf(
            ContextCompat.getColor(requireContext(), R.color.hisab_brown),
            ContextCompat.getColor(requireContext(), R.color.hisab_yellow),
            ContextCompat.getColor(requireContext(), R.color.hisab_green)
        )
        val entries = data.entries.take(3).map { PieEntry(it.value.toFloat(), it.key) }
        val total = data.values.sum()
        val dataSet = PieDataSet(entries, "").apply {
            colors = palette
            valueTextColor = Color.TRANSPARENT
            sliceSpace = 0f
            selectionShift = 0f
        }
        chart.data = PieData(dataSet)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.isDrawHoleEnabled = true
        chart.holeRadius = 72f
        chart.transparentCircleRadius = 0f
        chart.setHoleColor(Color.WHITE)
        chart.setDrawEntryLabels(false)

        val totalFormatted = viewModel.formatCurrencyNoDecimals(total)
        val centerTextStr = "$totalFormatted\n${getString(R.string.spent)}"
        val spannable = android.text.SpannableString(centerTextStr).apply {
            setSpan(android.text.style.RelativeSizeSpan(1.6f), 0, totalFormatted.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, totalFormatted.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(android.text.style.ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.hisab_text_primary)), 0, totalFormatted.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            
            val startSpent = totalFormatted.length + 1
            val endSpent = centerTextStr.length
            setSpan(android.text.style.RelativeSizeSpan(0.85f), startSpent, endSpent, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(android.text.style.ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.hisab_text_secondary)), startSpent, endSpent, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        chart.centerText = spannable
        chart.invalidate()
    }

    private fun renderLegend(data: Map<String, Double>) {
        val topThree = data.entries.sortedByDescending { it.value }.take(3)
        val total = data.values.sum().takeIf { it > 0.0 } ?: 1.0
        val rows = listOf(
            Triple(binding.legendRow1, binding.legendText1, binding.legendValue1),
            Triple(binding.legendRow2, binding.legendText2, binding.legendValue2),
            Triple(binding.legendRow3, binding.legendText3, binding.legendValue3)
        )
        rows.forEachIndexed { index, triple ->
            val entry = topThree.getOrNull(index)
            triple.first.isVisible = entry != null
            if (entry != null) {
                triple.second.text = entry.key
                triple.third.text = "${((entry.value / total) * 100).toInt()}%"
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
