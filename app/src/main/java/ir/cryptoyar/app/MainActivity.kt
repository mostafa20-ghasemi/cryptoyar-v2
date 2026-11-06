
package ir.cryptoyar.app4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

val DarkBg = Color(0xFF121212)
val CardBg = Color(0xFF1E1E1E)
val TextLight = Color(0xFFF5F5F5)
val TextGray = Color(0xFFAAAAAA)
val CryptoBlue = Color(0xFF2196F3)
val SellRed = Color(0xFFF44336)
val BuyGreen = Color(0xFF4CAF50)
val WaitAmber = Color(0xFFFFC107)
val MIN_CONFIDENCE = 50

data class Analysis(
    val direction: String,
    val confidence: Int,
    val reason: String,
    val price: Double,
    val rsi: Double,
    val ema20: Double?,
    val ema50: Double?,
    val ema200: Double?,
    val atr: Double,
    val entryLow: Double,
    val entryHigh: Double,
    val sl: Double,
    val tp1: Double,
    val tp2: Double,
    val rr: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = DarkBg, modifier = Modifier.fillMaxSize()) {
                    AppScreen()
                }
            }
        }
    }
}

@Composable
fun AppScreen() {
    var analysis by remember { mutableStateOf<Analysis?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        error = null
        try {
            analysis = Analyzer.fetch()
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("CryptoYar v2", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextLight)
        Text("تحلیل BTC/USDT — تایم فریم ۱۵ دقیقه", color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

        Button(
            onClick = { refreshTrigger++ },
            colors = ButtonDefaults.buttonColors(containerColor = CryptoBlue),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("🔄 تحلیل مجدد", color = Color.White, fontSize = 16.sp)
        }

        if (isLoading) {
            CircularProgressIndicator(color = CryptoBlue)
        } else if (error != null) {
            Text("خطا: $error", color = SellRed, fontSize = 16.sp)
        } else {
            analysis?.let { a ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.bitcoin_icon),
                            contentDescription = "Bitcoin Icon",
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("قیمت لحظه‌ای", color = TextGray, fontSize = 14.sp)
                        Text(text = String.format("$%.0f", a.price), color = TextLight, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        
                        val signalText = when (a.direction) { "BUY" -> "خرید"; "SELL" -> "فروش"; else -> "صبر" }
                        val signalColor = when (a.direction) { "BUY" -> BuyGreen; "SELL" -> SellRed; else -> WaitAmber }
                        val signalEmoji = when (a.direction) { "BUY" -> "🟢"; "SELL" -> "🔴"; else -> "🟡" }

                        Box(
                            modifier = Modifier
                                .background(signalColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(signalEmoji, fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(text = signalText, color = signalColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📊", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("تحلیل", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(a.reason, color = TextGray, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (a.direction != "WAIT") {
                    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎯", fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("محدوده معامله", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(14.dp))
                            RowLine("Entry", String.format("%.0f - %.0f", a.entryLow, a.entryHigh), CryptoBlue)
                            RowLine("Stop Loss", String.format("%.0f", a.sl), SellRed)
                            RowLine("Take Profit 1", String.format("%.0f", a.tp1), BuyGreen)
                            RowLine("Take Profit 2", String.format("%.0f", a.tp2), BuyGreen)
                            Divider(color = TextGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
                            RowLine("Risk : Reward", String.format("1 : %.2f", a.rr), if (a.rr >= 2.0) BuyGreen else WaitAmber)
                            RowLine("ATR (نوسان)", String.format("%.0f", a.atr), TextGray)
                            RowLine("RSI", String.format("%.1f", a.rsi), when { a.rsi > 70 -> SellRed; a.rsi < 30 -> BuyGreen; else -> TextGray })
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(colors = CardDefaults.cardColors(containerColor = WaitAmber.copy(alpha = 0.10f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("⚠️ مسئولیت تصمیم با شماست. این پیشنهاد است، نه دستور معامله.", color = WaitAmber, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun RowLine(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextGray, fontSize = 14.sp)
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

object Analyzer {
    suspend fun fetch(): Analysis = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
        val now = System.currentTimeMillis() / 1000
        val url = "https://apiv2.nobitex.ir/market/udf/history?symbol=BTCIRT&resolution=15&to=$now&countback=300"
        
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("پاسخ خالی از سرور نوبیتکس")

        if (!response.isSuccessful) throw Exception("خطای شبکه نوبیتکس: " + response.code)

        val json = JSONObject(body)
        if (json.optString("s") != "ok") throw Exception("داده‌ای از نوبیتکس دریافت نشد")
        
        val closesJson = json.getJSONArray("c")
        val highsJson = json.getJSONArray("h")
        val lowsJson = json.getJSONArray("l")

        val closes = mutableListOf<Double>()
        val highs = mutableListOf<Double>()
        val lows = mutableListOf<Double>()

        for (i in 0 until closesJson.length()) {
            closes.add(closesJson.getDouble(i))
            highs.add(highsJson.getDouble(i))
            lows.add(lowsJson.getDouble(i))
        }

        if (closes.isEmpty()) throw Exception("داده‌ای یافت نشد")
        val price = closes.last()

        val rsiVal = rsi(closes, 14)
        val ema20Val = ema(closes, 20)
        val ema50Val = ema(closes, 50)
        val ema200Val = ema(closes, 200)
        val atrVal = atr(highs, lows, closes, 14)

        var score = 0
        val reasons = mutableListOf<String>()

        if (rsiVal > 70) {
            score -= 10
            reasons.add(String.format("🔻 RSI اشباع خرید (%.1f)", rsiVal))
        } else if (rsiVal < 30) {
            score += 10
            reasons.add(String.format("✅ RSI اشباع فروش (%.1f)", rsiVal))
        } else {
            reasons.add(String.format("➖ RSI خنثی (%.1f)", rsiVal))
        }

        if (ema20Val != null && ema50Val != null) {
            if (ema20Val > ema50Val) {
                score += 15
                reasons.add("✅ کراس صعودی EMA20 و EMA50")
            } else {
                score -= 15
                reasons.add("🔻 کراس نزولی EMA20 و EMA50")
            }
        }

        if (ema200Val != null) {
            if (price > ema200Val) {
                score += 8
                reasons.add("✅ قیمت بالای EMA200")
            } else {
                score -= 8
                reasons.add("🔻 قیمت زیر EMA200")
            }
        }

        if (closes.size >= 6) {
            val momentum = price - closes[closes.size - 6]
            val momentumRatio = if (atrVal > 0) momentum / atrVal else 0.0
            if (momentumRatio > 0.5) {
                score += 15
                reasons.add("✅ مومنتوم صعودی قوی")
            } else if (momentumRatio < -0.5) {
                score -= 15
                reasons.add("🔻 مومنتوم نزولی قوی")
            }
        }

        var direction = when {
            score >= 40 -> "BUY"
            score <= -40 -> "SELL"
            else -> "WAIT"
        }

        var confidence = minOf(95, abs(score) + 40)

        if ((direction == "BUY" || direction == "SELL") && confidence < MIN_CONFIDENCE) {
            direction = "WAIT"
            reasons.clear()
            reasons.add("⚠️ قدرت سیگنال زیر $MIN_CONFIDENCE%")
        }

        val entryLow = price * 0.998
        val entryHigh = price * 1.002
        val sl = if (direction == "BUY") price - (atrVal * 1.5) else price + (atrVal * 1.5)
        val tp1 = if (direction == "BUY") price + (atrVal * 1.5) else price - (atrVal * 1.5)
        val tp2 = if (direction == "BUY") price + (atrVal * 3.0) else price - (atrVal * 3.0)
        val rr = 2.0

        Analysis(direction, confidence, reasons.joinToString("\n"), price, rsiVal, ema20Val, ema50Val, ema200Val, atrVal, entryLow, entryHigh, sl, tp1, tp2, rr)
    }

    fun ema(values: List<Double>, period: Int): Double? {
        if (values.size < period) return null
        val k = 2.0 / (period + 1)
        var e = values.take(period).average()
        for (i in period until values.size) {
            e = values[i] * k + e * (1 - k)
        }
        return e
    }

    fun rsi(values: List<Double>, period: Int): Double {
        if (values.size <= period) return 50.0
        var gains = 0.0
        var losses = 0.0
        for (i in 1..period) {
            val diff = values[i] - values[i - 1]
            if (diff > 0) gains += diff else losses -= diff
        }
        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in period + 1 until values.size) {
            val diff = values[i] - values[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        return 100.0 - (100.0 / (1.0 + (avgGain / avgLoss)))
    }

    fun atr(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int): Double {
        if (closes.size <= period) return 0.0
        val trs = ArrayList<Double>(closes.size - 1)
        for (i in 1 until closes.size) {
            val highLow = highs[i] - lows[i]
            val highClose = Math.abs(highs[i] - closes[i - 1])
            val lowClose = Math.abs(lows[i] - closes[i - 1])
            trs.add(maxOf(highLow, highClose, lowClose))
        }
        var atrVal = trs.take(period).average()
        for (i in period until trs.size) {
            atrVal = (atrVal * (period - 1) + trs[i]) / period
        }
        return atrVal
    }
}