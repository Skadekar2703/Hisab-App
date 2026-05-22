package com.tommy.hisabapp.data.model

data class TransactionItem(
    val id: String = "",
    val uid: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
