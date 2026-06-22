package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist_items ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WatchlistItem>>

    @Query("SELECT * FROM watchlist_items ORDER BY addedAt DESC")
    suspend fun getAllList(): List<WatchlistItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistItem)

    @Delete
    suspend fun delete(item: WatchlistItem)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_items WHERE symbol = :symbol LIMIT 1)")
    suspend fun exists(symbol: String): Boolean
}

@Dao
interface PriceAlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PriceAlert>>

    @Query("SELECT * FROM price_alerts WHERE isActive = 1")
    suspend fun getAllActive(): List<PriceAlert>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: PriceAlert)

    @Update
    suspend fun update(alert: PriceAlert)

    @Delete
    suspend fun delete(alert: PriceAlert)
}

@Dao
interface ScalpSignalDao {
    @Query("SELECT * FROM scalp_signals ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ScalpSignal>>

    @Query("SELECT * FROM scalp_signals ORDER BY timestamp DESC")
    suspend fun getAllList(): List<ScalpSignal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(signal: ScalpSignal)

    @Update
    suspend fun update(signal: ScalpSignal)

    @Query("DELETE FROM scalp_signals")
    suspend fun clear()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChat(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clear()
}

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys")
    suspend fun getAll(): List<ApiKeyInfo>

    @Query("SELECT * FROM api_keys WHERE keyType = :keyType LIMIT 1")
    suspend fun getByKeyType(keyType: String): ApiKeyInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keyInfo: ApiKeyInfo)

    @Query("DELETE FROM api_keys WHERE keyType = :keyType")
    suspend fun delete(keyType: String)
}

@Database(
    entities = [
        WatchlistItem::class,
        PriceAlert::class,
        ScalpSignal::class,
        ChatMessage::class,
        ApiKeyInfo::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TradeDatabase : RoomDatabase() {
    abstract val watchlistDao: WatchlistDao
    abstract val priceAlertDao: PriceAlertDao
    abstract val scalpSignalDao: ScalpSignalDao
    abstract val chatDao: ChatDao
    abstract val apiKeyDao: ApiKeyDao
}
