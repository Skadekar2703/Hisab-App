package com.tommy.hisabapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tommy.hisabapp.data.AuthRepository
import com.tommy.hisabapp.data.TransactionRepository
import com.tommy.hisabapp.data.model.TransactionItem
import com.tommy.hisabapp.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private var transactionObserverJob: Job? = null

    private val _uiState = MutableStateFlow(
        MainUiState(
            isAuthenticated = authRepository.isLoggedIn(),
            userName = authRepository.currentUserName(),
            userEmail = authRepository.currentUserEmail()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        if (authRepository.isLoggedIn()) {
            startTransactionObserver()
            updateReport()
        }
    }

    fun refreshSession() {
        val loggedIn = authRepository.isLoggedIn()
        _uiState.update {
            it.copy(
                isAuthenticated = loggedIn,
                userName = authRepository.currentUserName(),
                userEmail = authRepository.currentUserEmail()
            )
        }
        if (loggedIn) {
            startTransactionObserver()
        }
    }

    fun onGoogleSignInSuccess(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching { authRepository.signInWithGoogle(idToken) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userName = authRepository.currentUserName(),
                            userEmail = authRepository.currentUserEmail(),
                            message = "Signed in successfully."
                        )
                    }
                    startTransactionObserver()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, message = error.message ?: "Unable to sign in.")
                    }
                }
        }
    }

    fun onEmailSignIn(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching { authRepository.signInWithEmail(email) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userName = authRepository.currentUserName(),
                            userEmail = authRepository.currentUserEmail(),
                            message = "Signed in successfully."
                        )
                    }
                    startTransactionObserver()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, message = error.message ?: "Unable to sign in.")
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
                .onSuccess {
                    _uiState.value = MainUiState(message = "Logged out.")
                }
                .onFailure {
                    _uiState.update { current ->
                        current.copy(message = it.message ?: "Unable to logout.")
                    }
                }
        }
    }

    fun saveTransaction(
        amountText: String,
        category: String,
        note: String,
        type: TransactionType,
        date: Long
    ) {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(message = "Enter a valid amount.") }
            return
        }
        if (category.isBlank()) {
            _uiState.update { it.copy(message = "Choose a category or transfer type.") }
            return
        }
        val uid = authRepository.currentUserUid()
        if (uid.isBlank()) {
            _uiState.update { it.copy(message = "Sign in to save transactions.") }
            return
        }

        viewModelScope.launch {
            runCatching {
                transactionRepository.addTransaction(
                    TransactionItem(
                        uid = uid,
                        type = type,
                        category = category,
                        amount = amount,
                        note = note.trim(),
                        date = date,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(message = "Transaction saved.") }
            }.onFailure {
                _uiState.update { it.copy(message = it.message ?: "Unable to save transaction.") }
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            runCatching { transactionRepository.deleteTransaction(id) }
                .onFailure {
                    _uiState.update { it.copy(message = it.message ?: "Unable to delete transaction.") }
                }
        }
    }

    fun setReportMode(byDay: Boolean) {
        _uiState.update { it.copy(reportByDay = byDay) }
        updateReport()
    }

    fun setReportTime(timeInMillis: Long) {
        _uiState.update { it.copy(selectedReportTime = timeInMillis) }
        updateReport()
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return formatter.format(amount)
    }

    fun formatCurrencyNoDecimals(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        formatter.maximumFractionDigits = 0
        return formatter.format(amount)
    }

    fun formatDate(timeInMillis: Long): String {
        val now = Calendar.getInstance()
        val itemCalendar = Calendar.getInstance().apply { this.timeInMillis = timeInMillis }
        
        val isToday = now.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)
                
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)
                
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timeInMillis))
        
        return when {
            isToday -> "Today, $timeFormat"
            isYesterday -> "Yesterday, $timeFormat"
            else -> {
                val diffDays = ((now.timeInMillis - itemCalendar.timeInMillis) / (24 * 3600 * 1000)).toInt()
                if (diffDays in 2..7) {
                    "$diffDays days ago"
                } else {
                    SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(timeInMillis))
                }
            }
        }
    }

    private fun startTransactionObserver() {
        val uid = authRepository.currentUserUid()
        if (uid.isBlank()) return

        transactionObserverJob?.cancel()
        transactionObserverJob = viewModelScope.launch {
            transactionRepository.observeTransactions(uid)
                .catch { error ->
                    _uiState.update { it.copy(message = error.message ?: "Unable to load data.") }
                }
                .collect { items ->
                    val finalItems = if (items.isEmpty()) {
                        val burgerKingTime = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 14)
                            set(Calendar.MINUTE, 30)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        val salaryTime = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -1)
                            set(Calendar.HOUR_OF_DAY, 10)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        val metroTime = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -1)
                            set(Calendar.HOUR_OF_DAY, 18)
                            set(Calendar.MINUTE, 15)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        val amazonTime = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -2)
                            set(Calendar.HOUR_OF_DAY, 15)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        listOf(
                            TransactionItem(
                                id = "mock_1",
                                uid = uid,
                                type = TransactionType.EXPENSE,
                                category = "Food",
                                amount = 450.0,
                                note = "Burger King",
                                date = burgerKingTime
                            ),
                            TransactionItem(
                                id = "mock_2",
                                uid = uid,
                                type = TransactionType.DEPOSIT,
                                category = "Salary",
                                amount = 12000.0,
                                note = "Salary Deposit",
                                date = salaryTime
                            ),
                            TransactionItem(
                                id = "mock_3",
                                uid = uid,
                                type = TransactionType.EXPENSE,
                                category = "Travel",
                                amount = 200.0,
                                note = "Metro Recharge",
                                date = metroTime
                            ),
                            TransactionItem(
                                id = "mock_4",
                                uid = uid,
                                type = TransactionType.EXPENSE,
                                category = "Shopping",
                                amount = 1299.0,
                                note = "Amazon Shopping",
                                date = amazonTime
                            )
                        )
                    } else {
                        items
                    }

                    _uiState.update {
                        it.copy(
                            transactions = finalItems,
                            balance = finalItems.sumOf { transaction -> transaction.amount * transaction.type.affectsBalance() }
                        )
                    }
                    updateReport()
                }
        }
    }

    private fun updateReport() {
        val current = _uiState.value
        val filtered = current.transactions.filter { transaction ->
            isSameWindow(
                timestamp = transaction.date,
                selected = current.selectedReportTime,
                byDay = current.reportByDay
            )
        }
        val chartData = filtered
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category.ifBlank { "Other" } }
            .mapValues { (_, values) -> values.sumOf { it.amount } }

        _uiState.update {
            it.copy(
                reportTransactions = filtered,
                reportPieData = chartData,
                reportLabel = if (current.reportByDay) {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(current.selectedReportTime))
                } else {
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(current.selectedReportTime))
                }
            )
        }
    }

    private fun isSameWindow(timestamp: Long, selected: Long, byDay: Boolean): Boolean {
        val transactionCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val selectedCalendar = Calendar.getInstance().apply { timeInMillis = selected }
        return if (byDay) {
            transactionCalendar.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
                transactionCalendar.get(Calendar.DAY_OF_YEAR) == selectedCalendar.get(Calendar.DAY_OF_YEAR)
        } else {
            transactionCalendar.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
                transactionCalendar.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH)
        }
    }
}

class MainViewModelFactory(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(authRepository, transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
