package com.tommy.hisabapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.tommy.hisabapp.data.AuthRepository
import com.tommy.hisabapp.data.TransactionRepository
import com.tommy.hisabapp.databinding.ActivityMainBinding
import com.tommy.hisabapp.ui.MainViewModel
import com.tommy.hisabapp.ui.MainViewModelFactory
import com.tommy.hisabapp.ui.add.AddTransactionFragment
import com.tommy.hisabapp.ui.home.HomeFragment
import com.tommy.hisabapp.ui.reports.ReportsFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authRepository by lazy { AuthRepository(applicationContext) }

    val viewModelFactory: MainViewModelFactory by lazy {
        MainViewModelFactory(authRepository, TransactionRepository())
    }

    private val viewModel: MainViewModel by viewModels { viewModelFactory }
    private var currentTab: NavTab = NavTab.HOME
    private var splashDismissed: Boolean = false

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                Toast.makeText(this, "Google sign-in was cancelled.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            handleGoogleResult(result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        splashDismissed = savedInstanceState?.getBoolean(KEY_SPLASH_DISMISSED) ?: false
        currentTab = savedInstanceState?.getString(KEY_CURRENT_TAB)
            ?.let { saved -> NavTab.entries.firstOrNull { it.name == saved } }
            ?: NavTab.HOME

        applyInsets()
        setupActions()
        observeState()
        viewModel.refreshSession()
        renderShell(viewModel.uiState.value.isAuthenticated)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SPLASH_DISMISSED, splashDismissed)
        outState.putString(KEY_CURRENT_TAB, currentTab.name)
    }

    fun navigateHome() {
        switchTab(NavTab.HOME)
    }

    fun showLogoutPrompt() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage("Sign out from Hisab on this device?")
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout) { _, _ -> viewModel.logout() }
            .show()
    }

    fun currentUserInitial(): String {
        val name = viewModel.uiState.value.userName
        return name.firstOrNull()?.uppercase() ?: "H"
    }

    private fun setupActions() {
        binding.getStartedButton.setOnClickListener {
            android.util.Log.d("HisabDebug", "getStartedButton clicked, splashDismissed = true")
            splashDismissed = true
            renderShell(viewModel.uiState.value.isAuthenticated)
        }
        binding.loginButton.setOnClickListener {
            googleSignInLauncher.launch(authRepository.signInIntent())
        }
        binding.continueButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.onEmailSignIn(email)
        }
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchTab(NavTab.HOME)
                R.id.nav_add -> switchTab(NavTab.ADD)
                R.id.nav_reports -> switchTab(NavTab.REPORTS)
            }
            true
        }
        binding.bottomNavigation.itemActiveIndicatorColor = android.content.res.ColorStateList.valueOf(
            androidx.core.content.ContextCompat.getColor(this, R.color.hisab_orange)
        )

        // Format terms text with color and underline
        val termsText = binding.loginTermsText.text.toString()
        val spannable = android.text.SpannableStringBuilder(termsText)
        val termsOfService = "Terms of Service"
        val privacyPolicy = "Privacy Policy"
        val color = androidx.core.content.ContextCompat.getColor(this, R.color.hisab_primary)

        val tosIndex = termsText.indexOf(termsOfService)
        if (tosIndex != -1) {
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(color),
                tosIndex,
                tosIndex + termsOfService.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                android.text.style.UnderlineSpan(),
                tosIndex,
                tosIndex + termsOfService.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val ppIndex = termsText.indexOf(privacyPolicy)
        if (ppIndex != -1) {
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(color),
                ppIndex,
                ppIndex + privacyPolicy.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                android.text.style.UnderlineSpan(),
                ppIndex,
                ppIndex + privacyPolicy.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.loginTermsText.text = spannable
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, 0, bars.right, 0)
            binding.splashContainer.setPadding(
                binding.splashContainer.paddingLeft,
                bars.top,
                binding.splashContainer.paddingRight,
                binding.splashContainer.paddingBottom + bars.bottom
            )
            binding.loginContainer.setPadding(0, bars.top, 0, bars.bottom)
            binding.mainContainer.setPadding(0, bars.top, 0, 0)
            binding.bottomNavigation.setPadding(
                binding.bottomNavigation.paddingLeft,
                binding.bottomNavigation.paddingTop,
                binding.bottomNavigation.paddingRight,
                bars.bottom
            )
            insets
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    renderShell(state.isAuthenticated)
                    if (state.isAuthenticated && supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
                        switchTab(currentTab, force = true)
                    }
                    state.message?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }

    private fun renderShell(isAuthenticated: Boolean) {
        binding.splashContainer.isVisible = !splashDismissed
        binding.loginContainer.isVisible = splashDismissed && !isAuthenticated
        binding.mainContainer.isVisible = splashDismissed && isAuthenticated
        if (splashDismissed && isAuthenticated) {
            switchTab(currentTab, force = supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null)
        }
    }

    private fun switchTab(tab: NavTab, force: Boolean = false) {
        if (!force && currentTab == tab) return
        currentTab = tab
        updateBottomNavUi()
        val fragment = when (tab) {
            NavTab.HOME -> HomeFragment()
            NavTab.ADD -> AddTransactionFragment()
            NavTab.REPORTS -> ReportsFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun updateBottomNavUi() {
        binding.bottomNavigation.menu.findItem(
            when (currentTab) {
                NavTab.HOME -> R.id.nav_home
                NavTab.ADD -> R.id.nav_add
                NavTab.REPORTS -> R.id.nav_reports
            }
        ).isChecked = true
    }

    private fun handleGoogleResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) {
                Toast.makeText(this, "Google token was missing.", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.onGoogleSignInSuccess(token)
            }
        } catch (error: ApiException) {
            Toast.makeText(this, error.localizedMessage ?: "Unable to sign in.", Toast.LENGTH_SHORT).show()
        }
    }

    private enum class NavTab {
        HOME,
        ADD,
        REPORTS
    }

    companion object {
        private const val KEY_SPLASH_DISMISSED = "splash_dismissed"
        private const val KEY_CURRENT_TAB = "current_tab"
    }
}
