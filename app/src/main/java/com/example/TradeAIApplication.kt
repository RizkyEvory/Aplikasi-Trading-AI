package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.example.data.local.TradeDatabase
import com.example.data.repository.ChatRepository
import com.example.data.repository.KeyManager
import com.example.data.repository.MarketRepository
import com.example.data.repository.SignalRepository
import androidx.work.*
import java.util.concurrent.TimeUnit

class TradeAIApplication : Application() {

    lateinit var database: TradeDatabase
        private set

    lateinit var keyManager: KeyManager
        private set

    lateinit var marketRepository: MarketRepository
        private set

    lateinit var signalRepository: SignalRepository
        private set

    lateinit var chatRepository: ChatRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Room Database
        database = Room.databaseBuilder(
            applicationContext,
            TradeDatabase::class.java,
            "tradeai_pro_database"
        ).fallbackToDestructiveMigration().build()

        // Initialize Manual DI (Service Locator)
        keyManager = KeyManager(database.apiKeyDao)
        marketRepository = MarketRepository(database.watchlistDao, keyManager)
        signalRepository = SignalRepository(database.scalpSignalDao, marketRepository)
        chatRepository = ChatRepository(database.chatDao, keyManager)

        // Setup notification channel for background pushes
        createNotificationChannel()

        // Start Background Signals WorkManager
        scheduleBackgroundWork()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TradeAI Pro Signals"
            val descriptionText = "Real-time scalping signals alerts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("tradeai_signals_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleBackgroundWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<SignalWorker>(
            5, TimeUnit.MINUTES // Checks every 5 minutes in background
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "tradeai_periodic_signals_work",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    companion object {
        lateinit var instance: TradeAIApplication
            private set
    }
}

// Background Worker to generate signals and trigger notifications
class SignalWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as TradeAIApplication
            app.signalRepository.refreshSignalsForWatchlist()

            // Fetch last signal to push notification if it's new (timestamp within 5m)
            val allSignals = app.database.scalpSignalDao.getAllList()
            val latest = allSignals.firstOrNull()

            if (latest != null && latest.status == "OPEN" && (System.currentTimeMillis() - latest.timestamp < 300000)) {
                // Trigger notification push
                sendSignalNotification(latest.symbol, latest.direction, latest.entryPrice, latest.timeframe)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendSignalNotification(symbol: String, dir: String, entry: Double, tf: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = androidx.core.app.NotificationCompat.Builder(applicationContext, "tradeai_signals_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔥 NEW TradeAI Signal: $symbol ($tf)")
            .setContentText("Direction: $dir | Entry Target: $entry. Check scalp setups now!")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(445124, builder.build())
    }
}
