package com.tommy.hisabapp.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tommy.hisabapp.data.model.FirestoreTransactionDto
import com.tommy.hisabapp.data.model.TransactionItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TransactionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val transactions = firestore.collection("transactions")

    fun observeTransactions(uid: String): Flow<List<TransactionItem>> = callbackFlow {
        val listener = transactions
            .whereEqualTo("uid", uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents.orEmpty().mapNotNull { document ->
                    document.toObject(FirestoreTransactionDto::class.java)?.toDomain(document.id)
                }
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addTransaction(item: TransactionItem) {
        transactions.add(FirestoreTransactionDto.from(item)).await()
    }

    suspend fun deleteTransaction(id: String) {
        transactions.document(id).delete().await()
    }
}
