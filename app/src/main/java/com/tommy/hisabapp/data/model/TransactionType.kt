package com.tommy.hisabapp.data.model

enum class TransactionType {
    EXPENSE,
    DEPOSIT,
    TRANSFER_IN,
    TRANSFER_OUT;

    fun affectsBalance(): Double = when (this) {
        EXPENSE, TRANSFER_OUT -> -1.0
        DEPOSIT, TRANSFER_IN -> 1.0
    }

    fun displayName(): String = when (this) {
        EXPENSE -> "Expense"
        DEPOSIT -> "Deposit"
        TRANSFER_IN -> "Outside to Account"
        TRANSFER_OUT -> "Account to Outside"
    }
}
