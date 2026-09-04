package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object GoogleAuthHelper {

    private const val TAG = "GoogleAuthHelper"

    suspend fun signInWithGoogle(
        context: Context,
        webClientId: String? = null
    ): Result<FirebaseUser?> {
        val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        val credentialManager = CredentialManager.create(context)

        return try {
            val serverClientId = webClientId?.ifBlank { null }
                ?: try {
                    // Try getting default_web_client_id from string resources if available
                    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                    if (resId != 0) context.getString(resId) else "dummy-client-id"
                } catch (e: Exception) {
                    "dummy-client-id"
                }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                if (auth != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult: AuthResult = auth.signInWithCredential(firebaseCredential).await()
                    Result.success(authResult.user)
                } else {
                    Result.success(null)
                }
            } else {
                Result.failure(Exception("Unknown credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager error (offline or Play Services fallback): ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.w(TAG, "Sign in exception: ${e.message}")
            Result.failure(e)
        }
    }
}
