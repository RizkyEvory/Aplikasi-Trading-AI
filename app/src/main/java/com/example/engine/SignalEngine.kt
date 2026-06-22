package com.example.engine

import com.example.data.model.Candle
import com.example.data.model.ScalpSignal
import kotlin.math.sqrt

object SignalEngine {

    // Calculate EMA
    fun calculateEMA(candles: List<Candle>, period: Int): List<Double> {
        val ema = MutableList(candles.size) { 0.0 }
        if (candles.size < period) return ema

        // Simple SMA for first value
        var sma = 0.0
        for (i in 0 until period) {
            sma += candles[i].close
        }
        sma /= period
        ema[period - 1] = sma

        val multiplier = 2.0 / (period + 1)
        for (i in period until candles.size) {
            ema[i] = (candles[i].close - ema[i - 1]) * multiplier + ema[i - 1]
        }
        return ema
    }

    // Calculate RSI (Relative Strength Index)
    fun calculateRSI(candles: List<Candle>, period: Int = 14): List<Double> {
        val rsi = MutableList(candles.size) { 50.0 }
        if (candles.size < period + 1) return rsi

        var avgGain = 0.0
        var avgLoss = 0.0

        // first rsi
        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) {
                avgGain += change
            } else {
                avgLoss += -change
            }
        }

        avgGain /= period
        avgLoss /= period

        if (avgLoss == 0.0) {
            rsi[period] = 100.0
        } else {
            val rs = avgGain / avgLoss
            rsi[period] = 100.0 - (100.0 / (1.0 + rs))
        }

        for (i in (period + 1) until candles.size) {
            val change = candles[i].close - candles[i - 1].close
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) -change else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            if (avgLoss == 0.0) {
                rsi[i] = 100.0
            } else {
                val rs = avgGain / avgLoss
                rsi[i] = 100.0 - (100.0 / (1.0 + rs))
            }
        }
        return rsi
    }

    // Calculate MACD (Moving Average Convergence Divergence)
    // Returns: macdLine, signalLine, histogram
    fun calculateMACD(candles: List<Candle>): Triple<List<Double>, List<Double>, List<Double>> {
        val size = candles.size
        val macdLine = MutableList(size) { 0.0 }
        val signalLine = MutableList(size) { 0.0 }
        val histogram = MutableList(size) { 0.0 }

        if (size < 26) return Triple(macdLine, signalLine, histogram)

        val ema12 = calculateEMA(candles, 12)
        val ema26 = calculateEMA(candles, 26)

        for (i in 0 until size) {
            macdLine[i] = ema12[i] - ema26[i]
        }

        // Signal Line is 9-period EMA of MACD Line
        val signalMultiplier = 2.0 / (9 + 1)
        var sumMacd = 0.0
        for (i in 0 until 9) {
            sumMacd += macdLine[i]
        }
        signalLine[8] = sumMacd / 9

        for (i in 9 until size) {
            signalLine[i] = (macdLine[i] - signalLine[i - 1]) * signalMultiplier + signalLine[i - 1]
        }

        for (i in 0 until size) {
            histogram[i] = macdLine[i] - signalLine[i]
        }

        return Triple(macdLine, signalLine, histogram)
    }

    // Calculate Bollinger Bands
    // Returns: upperBand, middleBand (SMA), lowerBand
    fun calculateBollingerBands(
        candles: List<Candle>,
        period: Int = 20,
        stdDevMultiplier: Double = 2.0
    ): Triple<List<Double>, List<Double>, List<Double>> {
        val size = candles.size
        val upperBand = MutableList(size) { 0.0 }
        val middleBand = MutableList(size) { 0.0 }
        val lowerBand = MutableList(size) { 0.0 }

        if (size < period) return Triple(upperBand, middleBand, lowerBand)

        for (i in (period - 1) until size) {
            var sum = 0.0
            for (j in 0 until period) {
                sum += candles[i - j].close
            }
            val mean = sum / period
            middleBand[i] = mean

            var sumVariance = 0.0
            for (j in 0 until period) {
                val diff = candles[i - j].close - mean
                sumVariance += diff * diff
            }
            val stdDev = sqrt(sumVariance / period)

            upperBand[i] = mean + (stdDevMultiplier * stdDev)
            lowerBand[i] = mean - (stdDevMultiplier * stdDev)
        }

        return Triple(upperBand, middleBand, lowerBand)
    }

    // Calculate Average True Range (ATR)
    fun calculateATR(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size < period) return 0.0
        var totalTR = 0.0
        for (i in 1..period) {
            val h = candles[i].high
            val l = candles[i].low
            val pc = candles[i - 1].close
            val tr1 = h - l
            val tr2 = Math.abs(h - pc)
            val tr3 = Math.abs(l - pc)
            totalTR += maxOf(tr1, tr2, tr3)
        }
        var atr = totalTR / period
        for (i in (period + 1) until candles.size) {
            val h = candles[i].high
            val l = candles[i].low
            val pc = candles[i - 1].close
            val tr1 = h - l
            val tr2 = Math.abs(h - pc)
            val tr3 = Math.abs(l - pc)
            val tr = maxOf(tr1, tr2, tr3)
            atr = (atr * (period - 1) + tr) / period
        }
        return atr
    }

    // Generate signals base on indicators + ICT concept (FVG + OB)
    fun generateSignals(
        symbol: String,
        name: String,
        candles: List<Candle>,
        timeframe: String = "5m"
    ): List<ScalpSignal> {
        val signals = mutableListOf<ScalpSignal>()
        val size = candles.size
        if (size < 30) return signals

        val ema9 = calculateEMA(candles, 9)
        val ema21 = calculateEMA(candles, 21)
        val rsiVal = calculateRSI(candles, 14)
        val (macd, macdSignal, _) = calculateMACD(candles)
        val (bbUpper, _, bbLower) = calculateBollingerBands(candles, 20, 2.0)
        val atr = calculateATR(candles, 14)

        // Ensure volatility filter (ATR shouldn't be close to 0)
        val minVolatility = candles.last().close * 0.0005 // minimum 0.05% fluctuation
        if (atr < minVolatility) {
            // Skips signal generation on ultra-low volatility / consolidated range
            return signals
        }

        // Evaluate conditions on the last few candles (e.g. up to last index)
        for (i in (size - 3) until size) {
            val close = candles[i].close
            val prevClose = candles[i - 1].close

            // Indicator pemicu status values
            val isEma9CrossUpEma21 = (ema9[i] > ema21[i] && ema9[i - 1] <= ema21[i - 1])
            val isEma9CrossDownEma21 = (ema9[i] < ema21[i] && ema9[i - 1] >= ema21[i - 1])

            val isRsiOversold = rsiVal[i] < 35
            val isRsiOverbought = rsiVal[i] > 65

            val isMacdCrossUp = (macd[i] > macdSignal[i] && macd[i - 1] <= macdSignal[i - 1])
            val isMacdCrossDown = (macd[i] < macdSignal[i] && macd[i - 1] >= macdSignal[i - 1])

            val touchedLowerBand = candles[i].low <= bbLower[i]
            val touchedUpperBand = candles[i].high >= bbUpper[i]

            // ICT Fair Value Gap (FVG) Bullish: Gap between High of Candle 1 & Low of Candle 3
            // In candles sequence: high of candle [i-2] < low of candle [i]
            val isBullishFVG = candles[i].low > candles[i - 2].high && (candles[i - 1].close > candles[i - 1].open)
            // Bearish FVG: low of candle [i-2] > high of candle [i]
            val isBearishFVG = candles[i].high < candles[i - 2].low && (candles[i - 1].close < candles[i - 1].open)

            // ICT Order Block (OB): Support or supply zone
            // Bullish OB: last down candle close before strong upward swing
            val isBullishOB = (candles[i].close > candles[i - 1].high) && (candles[i - 1].close < candles[i - 1].open)

            // Calculate confidence score (weighted out of 100)
            var bullishIndicators = 0
            if (isEma9CrossUpEma21) bullishIndicators += 2
            if (isRsiOversold) bullishIndicators += 2
            if (isMacdCrossUp) bullishIndicators += 2
            if (touchedLowerBand) bullishIndicators += 1
            if (isBullishFVG) bullishIndicators += 2
            if (isBullishOB) bullishIndicators += 1

            var bearishIndicators = 0
            if (isEma9CrossDownEma21) bearishIndicators += 2
            if (isRsiOverbought) bearishIndicators += 2
            if (isMacdCrossDown) bearishIndicators += 2
            if (touchedUpperBand) bearishIndicators += 1
            if (isBearishFVG) bearishIndicators += 2

            // Decision
            val triggerTime = candles[i].timestamp

            if (bullishIndicators >= 3) {
                val confidence = (bullishIndicators.toDouble() / 10.0) * 100.0
                val triggers = mutableListOf<String>()
                if (isEma9CrossUpEma21) triggers.add("EMA Cross")
                if (isRsiOversold) triggers.add("RSI Oversold")
                if (isMacdCrossUp) triggers.add("MACD GoldenCross")
                if (isBullishFVG) triggers.add("ICT Bullish FVG")
                if (isBullishOB) triggers.add("ICT Order Block")

                val ep = close
                // Quick Scalping target SL/TP settings
                val sl = ep - (atr * 1.5)
                val tp = ep + (atr * 2.0)

                signals.add(
                    ScalpSignal(
                        symbol = symbol,
                        name = name,
                        direction = "BUY",
                        timeframe = timeframe,
                        entryPrice = ep,
                        takeProfit = tp,
                        stopLoss = sl,
                        confidence = confidence.coerceAtMost(100.0),
                        triggerIndication = triggers.joinToString(" + "),
                        timestamp = triggerTime,
                        status = "OPEN"
                    )
                )
            } else if (bearishIndicators >= 3) {
                val confidence = (bearishIndicators.toDouble() / 9.0) * 100.0
                val triggers = mutableListOf<String>()
                if (isEma9CrossDownEma21) triggers.add("EMA crossdown")
                if (isRsiOverbought) triggers.add("RSI Overbought")
                if (isMacdCrossDown) triggers.add("MACD DeathCross")
                if (isBearishFVG) triggers.add("ICT Bearish FVG")

                val ep = close
                val sl = ep + (atr * 1.5)
                val tp = ep - (atr * 2.0)

                signals.add(
                    ScalpSignal(
                        symbol = symbol,
                        name = name,
                        direction = "SELL",
                        timeframe = timeframe,
                        entryPrice = ep,
                        takeProfit = tp,
                        stopLoss = sl,
                        confidence = confidence.coerceAtMost(100.0),
                        triggerIndication = triggers.joinToString(" + "),
                        timestamp = triggerTime,
                        status = "OPEN"
                    )
                )
            }
        }

        // Avoid contradictory signals: filter if buy and sell appear on the same final set. Keep highest confidence
        if (signals.size > 1) {
            val sorted = signals.sortedByDescending { it.confidence }
            return listOf(sorted.first())
        }

        return signals
    }
}
