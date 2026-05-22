package com.tommy.hisabapp.ui

import com.tommy.hisabapp.R
import com.tommy.hisabapp.data.model.TransactionItem
import com.tommy.hisabapp.data.model.TransactionType

data class TransactionStyle(
    val iconRes: Int,
    val badgeBackgroundRes: Int,
    val chipBackgroundRes: Int,
    val chipTextColorRes: Int,
    val chipBackgroundColorRes: Int
)

object TransactionVisuals {
    fun styleFor(item: TransactionItem): TransactionStyle {
        val category = item.category.lowercase()
        return when {
            category.contains("food") || category.contains("cafe") -> TransactionStyle(
                iconRes = if (category.contains("cafe")) R.drawable.ic_coffee else R.drawable.ic_food,
                badgeBackgroundRes = R.drawable.bg_category_circle_pink,
                chipBackgroundRes = R.drawable.bg_tag_pink,
                chipTextColorRes = R.color.hisab_brown,
                chipBackgroundColorRes = R.color.hisab_chip_pink
            )
            category.contains("travel") || category.contains("metro") || category.contains("uber") || category.contains("transport") -> TransactionStyle(
                iconRes = R.drawable.ic_travel,
                badgeBackgroundRes = R.drawable.bg_category_circle_yellow,
                chipBackgroundRes = R.drawable.bg_tag_yellow,
                chipTextColorRes = R.color.hisab_brown,
                chipBackgroundColorRes = R.color.hisab_chip_yellow
            )
            category.contains("shop") -> TransactionStyle(
                iconRes = R.drawable.ic_shopping_bag,
                badgeBackgroundRes = R.drawable.bg_category_circle_yellow,
                chipBackgroundRes = R.drawable.bg_tag_yellow,
                chipTextColorRes = R.color.hisab_brown,
                chipBackgroundColorRes = R.color.hisab_chip_yellow
            )
            category.contains("entertain") || category.contains("fun") -> TransactionStyle(
                iconRes = R.drawable.ic_movie,
                badgeBackgroundRes = R.drawable.bg_category_circle_pink,
                chipBackgroundRes = R.drawable.bg_tag_pink,
                chipTextColorRes = R.color.hisab_brown,
                chipBackgroundColorRes = R.color.hisab_chip_pink
            )
            category.contains("college") -> TransactionStyle(
                iconRes = R.drawable.ic_college,
                badgeBackgroundRes = R.drawable.bg_category_circle_gray,
                chipBackgroundRes = R.drawable.bg_tag_gray,
                chipTextColorRes = R.color.hisab_brown,
                chipBackgroundColorRes = R.color.hisab_chip_gray
            )
            category.contains("hostel") -> TransactionStyle(
                iconRes = R.drawable.ic_hostel,
                badgeBackgroundRes = R.drawable.bg_category_circle_gray,
                chipBackgroundRes = R.drawable.bg_tag_gray,
                chipTextColorRes = R.color.hisab_brown,
                chipBackgroundColorRes = R.color.hisab_chip_gray
            )
            category.contains("recharge") -> TransactionStyle(
                iconRes = R.drawable.ic_recharge,
                badgeBackgroundRes = R.drawable.bg_category_circle_gray,
                chipBackgroundRes = R.drawable.bg_tag_gray,
                chipTextColorRes = R.color.hisab_brown,
                chipBackgroundColorRes = R.color.hisab_chip_gray
            )
            item.type == TransactionType.DEPOSIT || category.contains("salary") || category.contains("scholar") -> TransactionStyle(
                iconRes = R.drawable.ic_income,
                badgeBackgroundRes = R.drawable.bg_category_circle_green,
                chipBackgroundRes = R.drawable.bg_tag_green,
                chipTextColorRes = R.color.hisab_income,
                chipBackgroundColorRes = R.color.hisab_chip_green
            )
            category.contains("electric") || category.contains("utilit") -> TransactionStyle(
                iconRes = R.drawable.ic_utility,
                badgeBackgroundRes = R.drawable.bg_category_circle_green,
                chipBackgroundRes = R.drawable.bg_tag_green,
                chipTextColorRes = R.color.hisab_income,
                chipBackgroundColorRes = R.color.hisab_chip_green
            )
            else -> TransactionStyle(
                iconRes = when (item.type) {
                    TransactionType.TRANSFER_IN,
                    TransactionType.TRANSFER_OUT -> R.drawable.ic_transfer
                    TransactionType.DEPOSIT -> R.drawable.ic_income
                    TransactionType.EXPENSE -> R.drawable.ic_more_horiz
                },
                badgeBackgroundRes = R.drawable.bg_category_circle_gray,
                chipBackgroundRes = R.drawable.bg_tag_gray,
                chipTextColorRes = R.color.hisab_brown,
                chipBackgroundColorRes = R.color.hisab_chip_gray
            )
        }
    }
}
