package com.caltrack.app.data.repository

import com.caltrack.app.BuildConfig
import com.caltrack.app.data.local.UserPreferencesStore
import com.caltrack.app.data.remote.CognitoService
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val cognitoService: CognitoService,
    private val prefsStore: UserPreferencesStore
) {

    fun isDevMode(): Boolean = BuildConfig.COGNITO_CLIENT_ID.isEmpty()

    suspend fun signUp(email: String, password: String, name: String): Result<String> {
        return cognitoService.signUp(email, password, name).map { result ->
            result.codeDeliveryDestination
        }
    }

    suspend fun confirmSignUp(email: String, code: String): Result<Unit> {
        return cognitoService.confirmSignUp(email, code)
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return cognitoService.signIn(email, password).map { result ->
            prefsStore.saveTokens(result.idToken, result.accessToken, result.refreshToken)
            prefsStore.saveUserEmail(email)
        }
    }

    suspend fun signOut() {
        val accessToken = prefsStore.accessToken.firstOrNull()
        if (!accessToken.isNullOrBlank()) {
            cognitoService.globalSignOut(accessToken)
        }
        prefsStore.clearAll()
    }

    suspend fun isLoggedIn(): Boolean {
        val token = prefsStore.authToken.firstOrNull()
        return !token.isNullOrBlank()
    }
}
