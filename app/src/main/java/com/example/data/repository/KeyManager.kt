package com.example.data.repository

import com.example.data.local.ApiKeyDao
import com.example.data.model.ApiKeyInfo

class KeyManager(private val apiKeyDao: ApiKeyDao) {

    // Default built-in mock keys or empty values
    private val memoryKeys = mutableMapOf<String, MutableList<String>>()

    init {
        // Initial defaults can be configured or stored
    }

    suspend fun getApiKey(type: String): String {
        // Try to fetch from database first
        val dbKey = apiKeyDao.getByKeyType(type)
        if (dbKey != null && !dbKey.isLimitReached) {
            return dbKey.apiKeyValue
        }
        return ""
    }

    suspend fun saveApiKey(type: String, value: String) {
        val info = ApiKeyInfo(
            keyType = type,
            apiKeyValue = value,
            lastUsed = System.currentTimeMillis(),
            isLimitReached = false,
            requestCount = 0
        )
        apiKeyDao.insert(info)
    }

    suspend fun deleteApiKey(type: String) {
        apiKeyDao.delete(type)
    }

    suspend fun getAllKeys(): List<ApiKeyInfo> {
        return apiKeyDao.getAll()
    }

    // Handles rotating or marking failure
    suspend fun markLimitReached(type: String) {
        val keyInfo = apiKeyDao.getByKeyType(type)
        if (keyInfo != null) {
            val updated = keyInfo.copy(isLimitReached = true)
            apiKeyDao.insert(updated)
        }
    }

    suspend fun resetLimits() {
        val keys = apiKeyDao.getAll()
        for (k in keys) {
            apiKeyDao.insert(k.copy(isLimitReached = false, requestCount = 0))
        }
    }
}
