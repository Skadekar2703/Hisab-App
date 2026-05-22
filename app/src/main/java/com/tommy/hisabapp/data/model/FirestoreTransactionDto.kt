package com.tommy.hisabapp.data.model

data class FirestoreTransactionDto(
    val uid: String = "",
    val type: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val date: Long = 0L,
    val createdAt: Long = 0L
) {
    fun toDomain(id: String): TransactionItem {
        val parsedType = runCatching { TransactionType.valueOf(type) }
            .getOrDefault(TransactionType.EXPENSE)
        return TransactionItem(
            id = id,
            uid = uid,
            type = parsedType,
            category = category,
            amount = amount,
            note = note,
            date = date,
            createdAt = createdAt
        )
    }

    companion object {
        fun from(transaction: TransactionItem): FirestoreTransactionDto = FirestoreTransactionDto(
            uid = transaction.uid,
            type = transaction.type.name,
            category = transaction.category,
            amount = transaction.amount,
            note = transaction.note,
            date = transaction.date,
            createdAt = transaction.createdAt
        )
    }
}
