package com.tommy.hisabapp.ui.add

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.card.MaterialCardView
import com.tommy.hisabapp.MainActivity
import com.tommy.hisabapp.R
import com.tommy.hisabapp.data.model.TransactionType
import com.tommy.hisabapp.databinding.FragmentAddTransactionBinding
import com.tommy.hisabapp.ui.MainViewModel
import com.tommy.hisabapp.ui.TransactionVisuals
import java.util.Calendar

class AddTransactionFragment : Fragment() {
    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels {
        (requireActivity() as MainActivity).viewModelFactory
    }

    private val expenseCategories = listOf(
        "Food", "Travel", "Shopping", "Entertainment", "Recharge", "College", "Hostel", "Other"
    )
    private val depositCategories = listOf("Pocket Money", "Scholarship", "Refund", "Salary", "Other")
    private val transferCategories = listOf("Outside to Account", "Account to Outside")

    private var selectedDate = System.currentTimeMillis()
    private var selectedType: TransactionType = TransactionType.EXPENSE
    private var selectedCategory: String = expenseCategories.first()
    private var showAllCategories: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.avatarInitials.text = (requireActivity() as MainActivity).currentUserInitial()
        binding.backIcon.setOnClickListener { (requireActivity() as MainActivity).navigateHome() }
        binding.dateValue.text = viewModel.formatDate(selectedDate)
        binding.dateRow.setOnClickListener { showDatePicker() }
        binding.saveButton.setOnClickListener { saveTransaction() }

        binding.expenseButton.setOnClickListener { setTransactionType(TransactionType.EXPENSE) }
        binding.depositButton.setOnClickListener { setTransactionType(TransactionType.DEPOSIT) }
        binding.transferButton.setOnClickListener { setTransactionType(TransactionType.TRANSFER_IN) }
        binding.viewAllCategoriesText.setOnClickListener {
            showAllCategories = !showAllCategories
            binding.viewAllCategoriesText.text = if (showAllCategories) "Show Less" else getString(R.string.view_all)
            renderCategoryCards()
        }

        setTransactionType(TransactionType.EXPENSE)
    }

    private fun setTransactionType(type: TransactionType) {
        selectedType = type
        updateTypeButtons()
        renderCategoryCards()
    }

    private fun updateTypeButtons() {
        val selectedBg = ContextCompat.getColorStateList(requireContext(), R.color.hisab_orange)
        val unselectedBg = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
        val selectedText = ContextCompat.getColor(requireContext(), R.color.hisab_brown)
        val unselectedText = ContextCompat.getColor(requireContext(), R.color.hisab_text_secondary)

        listOf(
            binding.expenseButton to (selectedType == TransactionType.EXPENSE),
            binding.depositButton to (selectedType == TransactionType.DEPOSIT),
            binding.transferButton to (selectedType == TransactionType.TRANSFER_IN || selectedType == TransactionType.TRANSFER_OUT)
        ).forEach { (button, selected) ->
            button.backgroundTintList = if (selected) selectedBg else unselectedBg
            button.setTextColor(if (selected) selectedText else unselectedText)
            button.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.hisab_outline_light)
            button.strokeWidth = if (selected) 0 else 1
        }
    }

    private fun renderCategoryCards() {
        val categories = when (selectedType) {
            TransactionType.EXPENSE -> expenseCategories
            TransactionType.DEPOSIT -> depositCategories
            TransactionType.TRANSFER_IN, TransactionType.TRANSFER_OUT -> transferCategories
        }
        if (selectedCategory !in categories) selectedCategory = categories.first()
        binding.categoryContainer.removeAllViews()

        val displayedCategories = if (showAllCategories) categories else categories.take(4)
        val density = resources.displayMetrics.density
        val size48dp = (48 * density).toInt()
        val size22dp = (22 * density).toInt()
        val size10dp = (10 * density).toInt()

        displayedCategories.forEachIndexed { index, category ->
            val isSelected = selectedCategory == category
            val style = TransactionVisuals.styleFor(
                com.tommy.hisabapp.data.model.TransactionItem(category = category, type = selectedType)
            )

            val card = MaterialCardView(requireContext()).apply {
                radius = 24f * density
                cardElevation = 0f
                strokeWidth = 0
                setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        if (isSelected) style.chipBackgroundColorRes else R.color.hisab_surface_low
                    )
                )
                layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(index / 4, 1f),
                    GridLayout.spec(index % 4, 1f)
                ).apply {
                    width = 0
                    height = resources.getDimensionPixelSize(R.dimen.category_card_height)
                    val spacing = resources.getDimensionPixelSize(R.dimen.spacing_md)
                    setMargins(
                        if (index % 4 == 0) 0 else spacing / 2,
                        if (index < 4) 0 else spacing,
                        if (index % 4 == 3) 0 else spacing / 2,
                        0
                    )
                }
                setOnClickListener {
                    selectedCategory = category
                    if (category == "Account to Outside") selectedType = TransactionType.TRANSFER_OUT
                    if (category == "Outside to Account") selectedType = TransactionType.TRANSFER_IN
                    renderCategoryCards()
                }
            }

            val content = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(10, 12, 10, 12)
            }

            val badgeHolder = LinearLayout(requireContext()).apply {
                gravity = Gravity.CENTER
                background = ContextCompat.getDrawable(context,
                    if (isSelected) R.drawable.bg_category_circle_brown else R.drawable.bg_category_circle_gray
                )
                layoutParams = LinearLayout.LayoutParams(size48dp, size48dp)
            }

            val badge = ImageView(requireContext()).apply {
                setImageResource(style.iconRes)
                setColorFilter(ContextCompat.getColor(context,
                    if (isSelected) R.color.white else R.color.hisab_brown
                ))
                layoutParams = LinearLayout.LayoutParams(size22dp, size22dp)
            }

            val label = TextView(requireContext()).apply {
                text = category.replace("Shopping", "Shop").replace("Entertainment", "Fun")
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context,
                    if (isSelected) R.color.hisab_brown else R.color.hisab_text_secondary
                ))
                textSize = 11f
                maxLines = 2
                setPadding(0, size10dp, 0, 0)
            }

            badgeHolder.addView(badge)
            content.addView(badgeHolder)
            content.addView(label)
            card.addView(content)
            binding.categoryContainer.addView(card)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                selectedDate = calendar.timeInMillis
                binding.dateValue.text = viewModel.formatDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveTransaction() {
        val effectiveType = if (selectedCategory == "Account to Outside") {
            TransactionType.TRANSFER_OUT
        } else if (selectedCategory == "Outside to Account") {
            TransactionType.TRANSFER_IN
        } else {
            selectedType
        }

        viewModel.saveTransaction(
            amountText = binding.amountInput.text?.toString().orEmpty(),
            category = selectedCategory,
            note = binding.noteInput.text?.toString().orEmpty(),
            type = effectiveType,
            date = selectedDate
        )
        binding.amountInput.text?.clear()
        binding.noteInput.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
