package com.tommy.hisabapp.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.tommy.hisabapp.R
import kotlinx.coroutines.tasks.await

class AuthRepository(context: Context) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(context, gso)
    }

    private val prefs = context.getSharedPreferences("hisab_auth_prefs", Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null || prefs.getBoolean("is_logged_in", false)

    fun currentUserName(): String {
        val fbName = firebaseAuth.currentUser?.displayName
        if (!fbName.isNullOrEmpty()) return fbName
        return prefs.getString("user_name", "").orEmpty().ifBlank { "User" }
    }

    fun currentUserEmail(): String {
        val fbEmail = firebaseAuth.currentUser?.email
        if (!fbEmail.isNullOrEmpty()) return fbEmail
        return prefs.getString("user_email", "").orEmpty()
    }

    fun currentUserUid(): String {
        val fbUid = firebaseAuth.currentUser?.uid
        if (!fbUid.isNullOrEmpty()) return fbUid
        return prefs.getString("user_uid", "").orEmpty().ifBlank { "mock_uid_123" }
    }

    fun signInIntent() = googleClient.signInIntent

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
        prefs.edit().clear().apply()
    }

    suspend fun signInWithEmail(email: String) {
        try {
            firebaseAuth.signInAnonymously().await()
        } catch (e: Exception) {
            // Fallback to local mock if anonymous sign-in fails
        }
        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("user_uid", firebaseAuth.currentUser?.uid ?: "mock_uid_${email.hashCode()}")
            .apply()
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        try {
            googleClient.signOut().await()
        } catch (e: Exception) {
            // Ignore
        }
        prefs.edit().clear().apply()
    }
}
