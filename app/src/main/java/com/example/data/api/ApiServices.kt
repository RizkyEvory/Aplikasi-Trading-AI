package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers

// --- Twelve Data ---
data class TwelveSearchResponse(
    val data: List<TwelveSymbol>?,
    val status: String?
)

data class TwelveSymbol(
    val symbol: String,
    val name: String,
    val type: String, // Common Stock, Digital Currency, Physical Currency
    val currency: String?,
    val exchange: String?,
    val country: String?
)

data class TwelveSeriesResponse(
    val meta: TwelveMeta?,
    val values: List<TwelveCandleValue>?,
    val status: String?
)

data class TwelveMeta(
    val symbol: String,
    val interval: String,
    val type: String
)

data class TwelveCandleValue(
    val datetime: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String?
)

interface TwelveDataApiService {
    @GET("symbol_search")
    suspend fun searchSymbols(
        @Query("symbol") query: String,
        @Query("apikey") apiKey: String
    ): TwelveSearchResponse

    @GET("time_series")
    suspend fun getTimeSeries(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("outputsize") outputSize: Int = 50,
        @Query("apikey") apiKey: String
    ): TwelveSeriesResponse
}

// --- Finnhub ---
data class FinnhubCompanyProfile(
    val name: String?,
    val ticker: String?,
    val exchange: String?,
    val ipo: String?,
    val marketCapitalization: Double?,
    val shareOutstanding: Double?,
    val weburl: String?,
    val finnhubIndustry: String?
)

data class FinnhubQuote(
    val c: Double,  // Current price
    val d: Double?,  // Change
    val dp: Double?, // Percent change
    val h: Double?,  // High
    val l: Double?,  // Low
    val o: Double?,  // Open
    val pc: Double?  // Previous close
)

interface FinnhubApiService {
    @GET("stock/profile2")
    suspend fun getCompanyProfile(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubCompanyProfile

    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubQuote
}

// --- NewsAPI ---
data class NewsResponse(
    val status: String,
    val totalResults: Int?,
    val articles: List<NewsArticle>?
)

data class NewsArticle(
    val source: NewsSource?,
    val author: String?,
    val title: String?,
    val description: String?,
    val url: String?,
    val urlToImage: String?,
    val publishedAt: String?,
    val content: String?
)

data class NewsSource(
    val id: String?,
    val name: String?
)

interface NewsApiService {
    @GET("everything")
    suspend fun getNews(
        @Query("q") query: String = "trading OR stocks OR crypto OR forex",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 20,
        @Query("apiKey") apiKey: String
    ): NewsResponse
}

// --- OpenRouter Multi AI Chatbot API ---
data class OpenRouterMessage(
    val role: String,
    val content: String
)

data class OpenRouterChatRequest(
    val model: String = "openrouter/free",
    val messages: List<OpenRouterMessage>
)

data class OpenRouterChatResponse(
    val choices: List<OpenRouterChoice>?,
    val error: OpenRouterError?
)

data class OpenRouterChoice(
    val message: OpenRouterMessage?
)

data class OpenRouterError(
    val message: String?,
    val code: Int?
)

interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun checkCompletion(
        @Header("Authorization") bearerToken: String,
        @Body request: OpenRouterChatRequest
    ): OpenRouterChatResponse
}
