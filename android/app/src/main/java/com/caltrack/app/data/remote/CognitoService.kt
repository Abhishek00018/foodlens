package com.caltrack.app.data.remote

import com.caltrack.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class CognitoSignInResult(
    val idToken: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)

data class CognitoSignUpResult(
    val userSub: String,
    val codeDeliveryDestination: String
)

@Singleton
class CognitoService @Inject constructor() {

    private val client = OkHttpClient.Builder().build()
    private val endpoint = "https://cognito-idp.${BuildConfig.COGNITO_REGION}.amazonaws.com/"
    private val jsonMediaType = "application/x-amz-json-1.1".toMediaType()

    private fun isConfigured(): Boolean = BuildConfig.COGNITO_CLIENT_ID.isNotEmpty()

    private suspend fun post(target: String, body: JSONObject): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Content-Type", "application/x-amz-json-1.1")
                    .addHeader("X-Amz-Target", "AmazonCognitoIdentityProvider.$target")
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(JSONObject(responseBody))
                } else {
                    val errorJson = runCatching { JSONObject(responseBody) }.getOrNull()
                    val message = errorJson?.optString("message")
                        ?: errorJson?.optString("Message")
                        ?: "Cognito error ${response.code}"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun signUp(email: String, password: String, name: String): Result<CognitoSignUpResult> {
        if (!isConfigured()) return Result.failure(Exception("Cognito not configured"))
        val body = JSONObject().apply {
            put("ClientId", BuildConfig.COGNITO_CLIENT_ID)
            put("Username", email)
            put("Password", password)
            put("UserAttributes", org.json.JSONArray().apply {
                put(JSONObject().apply { put("Name", "email"); put("Value", email) })
                put(JSONObject().apply { put("Name", "name"); put("Value", name) })
            })
        }
        return post("SignUp", body).map { json ->
            val delivery = json.optJSONObject("CodeDeliveryDetails")
                ?.optString("Destination") ?: email
            CognitoSignUpResult(
                userSub = json.optString("UserSub"),
                codeDeliveryDestination = delivery
            )
        }
    }

    suspend fun confirmSignUp(email: String, code: String): Result<Unit> {
        if (!isConfigured()) return Result.failure(Exception("Cognito not configured"))
        val body = JSONObject().apply {
            put("ClientId", BuildConfig.COGNITO_CLIENT_ID)
            put("Username", email)
            put("ConfirmationCode", code)
        }
        return post("ConfirmSignUp", body).map { }
    }

    suspend fun signIn(email: String, password: String): Result<CognitoSignInResult> {
        if (!isConfigured()) return Result.failure(Exception("Cognito not configured"))
        val body = JSONObject().apply {
            put("AuthFlow", "USER_PASSWORD_AUTH")
            put("ClientId", BuildConfig.COGNITO_CLIENT_ID)
            put("AuthParameters", JSONObject().apply {
                put("USERNAME", email)
                put("PASSWORD", password)
            })
        }
        return post("InitiateAuth", body).map { json ->
            val result = json.getJSONObject("AuthenticationResult")
            CognitoSignInResult(
                idToken = result.getString("IdToken"),
                accessToken = result.getString("AccessToken"),
                refreshToken = result.optString("RefreshToken"),
                expiresIn = result.optInt("ExpiresIn", 3600)
            )
        }
    }

    suspend fun refreshTokens(refreshToken: String): Result<CognitoSignInResult> {
        if (!isConfigured()) return Result.failure(Exception("Cognito not configured"))
        val body = JSONObject().apply {
            put("AuthFlow", "REFRESH_TOKEN_AUTH")
            put("ClientId", BuildConfig.COGNITO_CLIENT_ID)
            put("AuthParameters", JSONObject().apply {
                put("REFRESH_TOKEN", refreshToken)
            })
        }
        return post("InitiateAuth", body).map { json ->
            val result = json.getJSONObject("AuthenticationResult")
            CognitoSignInResult(
                idToken = result.getString("IdToken"),
                accessToken = result.getString("AccessToken"),
                refreshToken = refreshToken,
                expiresIn = result.optInt("ExpiresIn", 3600)
            )
        }
    }

    suspend fun globalSignOut(accessToken: String): Result<Unit> {
        if (!isConfigured()) return Result.failure(Exception("Cognito not configured"))
        val body = JSONObject().apply {
            put("AccessToken", accessToken)
        }
        return post("GlobalSignOut", body).map { }
    }
}
