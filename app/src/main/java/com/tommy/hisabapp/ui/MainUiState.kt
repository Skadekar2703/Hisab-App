package com.tommy.hisabapp.ui

import com.tommy.hisabapp.data.model.TransactionItem

data class MainUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val balance: Double = 0.0,
    val transactions: List<TransactionItem> = emptyList(),
    val reportTransactions: List<TransactionItem> = emptyList(),
    val reportLabel: String = "",
    val reportPieData: Map<String, Double> = emptyMap(),
    val reportByDay: Boolean = false,
    val selectedReportTime: Long = System.currentTimeMillis(),
    val message: String? = null
)
