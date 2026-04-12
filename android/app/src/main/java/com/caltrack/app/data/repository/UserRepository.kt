package com.caltrack.app.data.repository

import com.caltrack.app.data.remote.AllergyRequest
import com.caltrack.app.data.remote.AllergyResponse
import com.caltrack.app.data.remote.CaltrackApi
import com.caltrack.app.data.remote.GoalRequest
import com.caltrack.app.data.remote.GoalResponse
import com.caltrack.app.data.remote.ProfileRequest
import com.caltrack.app.data.remote.ProfileResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: CaltrackApi
) {

    suspend fun getProfile(): Result<ProfileResponse> = safeCall {
        api.getProfile()
    }

    suspend fun updateProfile(request: ProfileRequest): Result<ProfileResponse> = safeCall {
        api.updateProfile(request)
    }

    suspend fun getGoals(): Result<GoalResponse> = safeCall {
        api.getGoals()
    }

    suspend fun updateGoals(request: GoalRequest): Result<GoalResponse> = safeCall {
        api.updateGoals(request)
    }

    suspend fun getAllergies(): Result<AllergyResponse> = safeCall {
        api.getAllergies()
    }

    suspend fun updateAllergies(allergies: List<String>): Result<AllergyResponse> = safeCall {
        api.updateAllergies(AllergyRequest(allergies))
    }

    private suspend fun <T> safeCall(
        block: suspend () -> retrofit2.Response<com.caltrack.app.data.remote.ApiEnvelope<T>>
    ): Result<T> {
        return try {
            val response = block()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val msg = response.body()?.error?.message ?: "Request failed (${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
