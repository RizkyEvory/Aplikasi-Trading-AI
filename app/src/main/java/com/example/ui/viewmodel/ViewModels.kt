package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.TradeAIApplication
import com.example.data.local.PriceAlertDao
import com.example.data.api.NewsArticle
import com.example.data.model.*
import com.example.data.repository.ChatRepository
import com.example.data.repository.KeyManager
import com.example.data.repository.MarketRepository
import com.example.data.repository.SignalRepository
import com.example.engine.SignalEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- MARKET VIEWMODEL (Search, Watchlist, Interactive Charts) ---
class MarketViewModel(
    private val marketRepository: MarketRepository
) : ViewModel() {

    val watchlist: StateFlow<List<WatchlistItem>> = marketRepository.watchlistStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchResults = MutableStateFlow<List<WatchlistItem>>(emptyList())
    val searchResults: StateFlow<List<WatchlistItem>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Interactive chart states
    private val _selectedSymbol = MutableStateFlow("BTC/USD")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _selectedAssetName = MutableStateFlow("Bitcoin")
    val selectedAssetName: StateFlow<String> = _selectedAssetName.asStateFlow()

    private val _selectedInterval = MutableStateFlow("5m")
    val selectedInterval: StateFlow<String> = _selectedInterval.asStateFlow()

    private val _chartCandles = MutableStateFlow<List<Candle>>(emptyList())
    val chartCandles: StateFlow<List<Candle>> = _chartCandles.asStateFlow()

    private val _isLoadingChart = MutableStateFlow(false)
    val isLoadingChart: StateFlow<Boolean> = _isLoadingChart.asStateFlow()

    // Technical indicators flags selected by user
    private val _showEMA = MutableStateFlow(true)
    val showEMA: StateFlow<Boolean> = _showEMA.asStateFlow()

    private val _showRSI = MutableStateFlow(false)
    val showRSI: StateFlow<Boolean> = _showRSI.asStateFlow()

    private val _showMACD = MutableStateFlow(false)
    val showMACD: StateFlow<Boolean> = _showMACD.asStateFlow()

    private val _showBB = MutableStateFlow(false)
    val showBB: StateFlow<Boolean> = _showBB.asStateFlow()

    private var pollingJob: Job? = null

    init {
        // Initial candle pull
        loadChartData()
        startPollingWatchlist()
    }

    fun startPollingWatchlist() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    marketRepository.refreshWatchlistPrices()
                } catch (e: Exception) {
                    // Ignore
                }
                delay(30000) // Update database with actual live prices every 30s
            }
        }
    }

    fun selectSymbol(symbol: String, name: String) {
        _selectedSymbol.value = symbol
        _selectedAssetName.value = name
        loadChartData()
    }

    fun selectInterval(interval: String) {
        _selectedInterval.value = interval
        loadChartData()
    }

    fun toggleEMA() { _showEMA.value = !_showEMA.value }
    fun toggleRSI() { _showRSI.value = !_showRSI.value }
    fun toggleMACD() { _showMACD.value = !_showMACD.value }
    fun toggleBB() { _showBB.value = !_showBB.value }

    fun loadChartData() {
        viewModelScope.launch {
            _isLoadingChart.value = true
            try {
                val data = marketRepository.getCandles(_selectedSymbol.value, _selectedInterval.value)
                _chartCandles.value = data
            } catch (e: Exception) {
                _chartCandles.value = emptyList()
            } finally {
                _isLoadingChart.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            val results = marketRepository.searchSymbols(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun toggleWatchlist(item: WatchlistItem) {
        viewModelScope.launch {
            marketRepository.toggleWatchlist(item.symbol, item.name, item.type)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}


// --- SIGNALS VIEWMODEL (Real-time polling, Win/Loss Status, History) ---
class SignalViewModel(
    private val signalRepository: SignalRepository
) : ViewModel() {

    val scalpSignals: StateFlow<List<ScalpSignal>> = signalRepository.allSignalsStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            // Seed defaults historical signals
            signalRepository.generateAndSaveMockSignals()
        }
        startPollingSignals()
    }

    fun startPollingSignals() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                _isRefreshing.value = true
                try {
                    signalRepository.refreshSignalsForWatchlist()
                } catch (e: Exception) {
                    // Ignore
                } finally {
                    _isRefreshing.value = false
                }
                // Polling every 30 seconds automatically when application tab is open
                delay(30000)
            }
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                signalRepository.refreshSignalsForWatchlist()
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearSignalHistory() {
        viewModelScope.launch {
            signalRepository.clearSignals()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}


// --- CHATBOT VIEWMODEL ---
class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatRepository.chatMessagesStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    init {
        // Send a friendly greeting if system is empty
        viewModelScope.launch {
            chatRepository.chatMessagesStream.firstOrNull()?.let { list ->
                if (list.isEmpty()) {
                    chatRepository.sendMessage("Hello! Give me a breakdown of the current market and explanation of your AI Scalping strategy.")
                }
            }
        }
    }

    fun askChatbot(prompt: String) {
        if (prompt.trim().isEmpty()) return
        viewModelScope.launch {
            _isAILoading.value = true
            try {
                chatRepository.sendMessage(prompt)
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearHistory()
        }
    }
}


// --- SETTINGS VIEWMODEL (Keys rotation & Price Alerts) ---
class SettingsViewModel(
    private val keyManager: KeyManager,
    private val alertDao: PriceAlertDao,
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _savedKeys = MutableStateFlow<Map<String, String>>(emptyMap())
    val savedKeys: StateFlow<Map<String, String>> = _savedKeys.asStateFlow()

    val priceAlerts: StateFlow<List<PriceAlert>> = alertDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _alertStatusMsg = MutableStateFlow("")
    val alertStatusMsg: StateFlow<String> = _alertStatusMsg.asStateFlow()

    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles.asStateFlow()

    private val _isLoadingNews = MutableStateFlow(false)
    val isLoadingNews: StateFlow<Boolean> = _isLoadingNews.asStateFlow()

    // Selected key values in form
    val twelveDataKeyInput = MutableStateFlow("")
    val finnhubKeyInput = MutableStateFlow("")
    val newsApiKeyInput = MutableStateFlow("")
    val openRouterKeyInput = MutableStateFlow("")

    init {
        loadKeys()
        loadNews()
    }

    fun loadKeys() {
        viewModelScope.launch {
            val list = keyManager.getAllKeys()
            val map = list.associate { it.keyType to it.apiKeyValue }
            _savedKeys.value = map

            // populate inputs
            twelveDataKeyInput.value = map["TWELVEDATA"] ?: ""
            finnhubKeyInput.value = map["FINNHUB"] ?: ""
            newsApiKeyInput.value = map["NEWSAPI"] ?: ""
            openRouterKeyInput.value = map["OPENROUTER"] ?: ""
        }
    }

    fun saveAllKeys() {
        viewModelScope.launch {
            keyManager.saveApiKey("TWELVEDATA", twelveDataKeyInput.value)
            keyManager.saveApiKey("FINNHUB", finnhubKeyInput.value)
            keyManager.saveApiKey("NEWSAPI", newsApiKeyInput.value)
            keyManager.saveApiKey("OPENROUTER", openRouterKeyInput.value)
            loadKeys()
        }
    }

    fun deleteKey(type: String) {
        viewModelScope.launch {
            keyManager.deleteApiKey(type)
            loadKeys()
        }
    }

    fun resetKeyLimits() {
        viewModelScope.launch {
            keyManager.resetLimits()
            loadKeys()
        }
    }

    fun createPriceAlert(symbol: String, price: Double, condition: String) {
        viewModelScope.launch {
            if (symbol.isBlank() || price <= 0) {
                _alertStatusMsg.value = "Invalid Price Action alert specifications."
                return@launch
            }
            alertDao.insert(
                PriceAlert(
                    symbol = symbol.uppercase(),
                    targetPrice = price,
                    condition = condition,
                    isActive = true
                )
            )
            _alertStatusMsg.value = "Succesfully saved alert for $symbol at $price!"
            delay(3000)
            _alertStatusMsg.value = ""
        }
    }

    fun deletePriceAlert(alert: PriceAlert) {
        viewModelScope.launch {
            alertDao.delete(alert)
        }
    }

    fun togglePriceAlertActive(alert: PriceAlert) {
        viewModelScope.launch {
            alertDao.update(alert.copy(isActive = !alert.isActive))
        }
    }

    fun loadNews() {
        viewModelScope.launch {
            _isLoadingNews.value = true
            try {
                val articles = marketRepository.getTradingNews()
                _newsArticles.value = articles
            } catch (e: Exception) {
                _newsArticles.value = emptyList()
            } finally {
                _isLoadingNews.value = false
            }
        }
    }
}


// --- VIEWMODEL PROVIDER FACTORY FOR MANUAL DI (Service Locator) ---
class ViewModelFactory(
    private val app: TradeAIApplication
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MarketViewModel::class.java) -> {
                MarketViewModel(app.marketRepository) as T
            }
            modelClass.isAssignableFrom(SignalViewModel::class.java) -> {
                SignalViewModel(app.signalRepository) as T
            }
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                ChatViewModel(app.chatRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(app.keyManager, app.database.priceAlertDao, app.marketRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
