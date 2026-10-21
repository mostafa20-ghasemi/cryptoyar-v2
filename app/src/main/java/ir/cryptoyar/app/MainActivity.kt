package ir.cryptoyar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

val CryptoBlue = Color(0xFF00B4D8)
val BgDark = Color(0xFF121826)
val CardBg = Color(0xFF1F2937)
val BuyGreen = Color(0xFF10B981)
val SellRed = Color(0xFFEF4444)
val WaitAmber = Color(0xFFF59E0B)
val TextGray = Color(0xFF9CA3AF)
val TextLight = Color(0xFFF9FAFB)

const val MIN_CONFIDENCE = 75
const val MIN_RR = 1.5

data class Analysis(
    val direction: String,
    val confidence: Int,
    val price: Double,
    val reason: String,
    val entryLow: Double,
    val entryHigh: Double,
    val sl: Double,
    val tp1: Double,
    val tp2: Double,
    val rr: Double,
    val atr: Double,
    val rsi: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { AppScreen() }
        }
    }
}

@Composable
fun AppScreen() {
    val scope = rememberCoroutineScope()
    var analysis by remember { mutableStateOf<Analysis?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastUpdate by remember { mutableStateOf("") }

    fun doAnalyze() {
        loading = true; error = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { Analyzer.fetch() }
                analysis = result
                lastUpdate = SimpleDateFormat("HH:mm:ss", Locale("fa")).format(Date())
            } catch (e: Exception) {
                error = e.message ?: "خطای شبکه"
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { doAnalyze() }

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("کریپتو یار", color = CryptoBlue, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("تحلیل BTC/IRT (نوبیتکس) — تایم فریم ۱۵ دقیقه", color = TextGray, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { doAnalyze() },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = CryptoBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = BgDark, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("🔄  تحلیل مجدد", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (lastUpdate.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("آخرین به‌روزرسانی: $lastUpdate", color = TextGray, fontSize = 11.sp)
        }
error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = SellRed.copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠️ خطا", color = SellRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = TextLight, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        analysis?.let { a ->
            val dirColor = when (a.direction) { "BUY" -> BuyGreen; "SELL" -> SellRed; else -> WaitAmber }
            val dirLabel = when (a.direction) { "BUY" -> "خرید"; "SELL" -> "فروش"; else -> "صبر" }
            val dirIcon = when (a.direction) { "BUY" -> "🟢"; "SELL" -> "🔴"; else -> "🟡" }

            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("قیمت لحظه‌ای", color = TextGray, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(String.format("%,.0f تومان", a.price), color = CryptoBlue, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(dirColor.copy(alpha = 0.18f)).padding(horizontal = 32.dp, vertical = 14.dp)) {
                        Text("$dirIcon  $dirLabel", color = dirColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    if (a.direction != "WAIT") {
                        Spacer(Modifier.height(10.dp))
                        Text("قدرت سیگنال: ${a.confidence}%", color = dirColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
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
                        RowLine("Entry", String.format("%,.0f تا %,.0f", a.entryLow, a.entryHigh), CryptoBlue)
                        RowLine("Stop Loss", String.format("%,.0f", a.sl), SellRed)
                        RowLine("Take Profit 1", String.format("%,.0f", a.tp1), BuyGreen)
                        RowLine("Take Profit 2", String.format("%,.0f", a.tp2), BuyGreen)
                        Divider(color = TextGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
                        RowLine("Risk : Reward", String.format("1 : %.2f", a.rr), if (a.rr >= 2.0) BuyGreen else WaitAmber)
                        RowLine("ATR (نوسان)", String.format("%,.0f تومان", a.atr), TextGray)
                        RowLine("RSI", String.format("%.1f", a.rsi), when { a.rsi > 70 -> SellRed; a.rsi < 30 -> BuyGreen; else -> TextGray })
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp), modifier = Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("تحلیل", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(a.reason, color = TextGray, fontSize = 14.sp, lineHeight = 22.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = WaitAmber.copy(alpha = 0.10f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text("⚠️ این پیشنهاد است، نه دستور معامله.\nمسئولیت تصمیم با شماست.", color = WaitAmber, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(14.dp))
        }
        Spacer(Modifier.height(20.dp))
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

        val closesArray = json.getJSONArray("c")
        val highsArray = json.getJSONArray("h")
        val lowsArray = json.getJSONArray("l")
        val n = closesArray.length()

        if (n < 60) throw Exception("داده کافی نیست")

        val closes = ArrayList<Double>(n)
        val highs = ArrayList<Double>(n)
        val lows = ArrayList<Double>(n)

        for (i in 0 until n) {
            closes.add(closesArray.getString(i).toDouble())
            highs.add(highsArray.getString(i).toDouble())
            lows.add(lowsArray.getString(i).toDouble())
        }

        val price = closes.last()
        val ema20 = ema(closes, 20)
        val ema50 = ema(closes, 50)
        val ema200 = if (closes.size >= 200) ema(closes, 200) else null
        val rsiVal = rsi(closes, 14)
        val atrVal = atr(highs, lows, closes, 14)

        var score = 0
        val reasons = ArrayList<String>()

        if (price > ema20 && ema20 > ema50) {
            score += 25
            reasons.add("✅ روند صعودی (EMA20 > EMA50)")
        } else if (price < ema20 && ema20 < ema50) {
            score -= 25
            reasons.add("🔻 روند نزولی (EMA20 < EMA50)")
        } else {
            reasons.add("⚠️ روند نامشخص")
        }

        if (rsiVal < 30) {
            score += 15
            reasons.add(String.format("✅ RSI اشباع فروش (%.1f)", rsiVal))
        } else if (rsiVal > 70) {
            score -= 15
            reasons.add(String.format("🔻 RSI اشباع خرید (%.1f)", rsiVal))
        } else if (rsiVal > 55) {
            score += 8
            reasons.add(String.format("✅ RSI صعودی (%.1f)", rsiVal))
        } else if (rsiVal < 45) {
            score -= 8
            reasons.add(String.format("🔻 RSI نزولی (%.1f)", rsiVal))
        } else {
            reasons.add(String.format("➖ RSI خنثی (%.1f)", rsiVal))
        }Modifier.fillMaxWidth()) {if (closes.size >= 6) {
            val momentum = price - closes[closes.size - 6]
            val momentumRatio = momentum / atrVal
            if (momentumRatio > 0.5) {
                score += 15
                reasons.add("✅ مومنتوم صعودی قوی")
            } else if (momentumRatio < -0.5) {
                score -= 15
                reasons.add("🔻 مومنتوم نزولی قوی")
            }
        }

        if (ema200 != null) {
            if (price > ema200) { score += 8; reasons.add("✅ بالای EMA200") }
            else { score -= 8; reasons.add("🔻 زیر EMA200") }
        }

        var direction = when { score >= 40 -> "BUY"; score <= -40 -> "SELL"; else -> "WAIT" }
        val confidence = minOf(95, abs(score) + 40)

        if ((direction == "BUY" || direction == "SELL") && confidence < MIN_CONFIDENCE) {
            direction = "WAIT"; reasons.clear(); reasons.add("⚠️ قدرت سیگنال زیر $MIN_CONFIDENCE%"); reasons.add("منتظر سیگنال قوی‌تر باشید")
        }

        var entryLow = 0.0; var entryHigh = 0.0; var sl = 0.0; var tp1 = 0.0; var tp2 = 0.0; var rr = 0.0

        if (direction == "BUY" || direction == "SELL") {
            if (direction == "BUY") {
                entryLow = price - atrVal * 0.15; entryHigh = price + atrVal * 0.15; val entryRef = entryHigh
                sl = entryRef - (atrVal * 1.5); tp1 = entryRef + (atrVal * 2.5); tp2 = entryRef + (atrVal * 4.0)
                rr = (tp1 - entryRef) / (entryRef - sl)
            } else {
                entryLow = price - atrVal * 0.15; entryHigh = price + atrVal * 0.15; val entryRef = entryLow
                sl = entryRef + (atrVal * 1.5); tp1 = entryRef - (atrVal * 2.5); tp2 = entryRef - (atrVal * 4.0)
                rr = (entryRef - tp1) / (sl - entryRef)
            }

            if (rr < MIN_RR) {
                direction = "WAIT"; reasons.clear(); reasons.add(String.format("⚠️ R:R پایین (1:%.2f)", rr)); reasons.add("منتظر ستاپ بهتر باشید"); rr = 0.0
            }
        }

        Analysis(direction, confidence, price, reasons.joinToString("\n"), entryLow, entryHigh, sl, tp1, tp2, rr, atrVal, rsiVal)
    }

    fun ema(values: List<Double>, period: Int): Double {
        if (values.size < period) return values.last()
        val k = 2.0 / (period + 1); var e = values.take(period).average()
        for (i in period until values.size) e = values[i] * k + e * (1 - k)
        return e
    }

    fun rsi(values: List<Double>, period: Int): Double {
        if (values.size < period + 1) return 50.0
        var gains = 0.0; var losses = 0.0
        for (i in 1..period) { val diff = values[i] - values[i - 1]; if (diff > 0) gains += diff else losses -= diff }
        var avgGain = gains / period; var avgLoss = losses / period
        for (i in period + 1 until values.size) {
            val diff = values[i] - values[i - 1]; val gain = if (diff > 0) diff else 0.0; val loss = if (diff < 0) -diff else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period; avgLoss = (avgLoss * (period - 1) + loss) / period
        }
        if (avgLoss == 0.0) return 100.0
        return 100.0 - (100.0 / (1.0 + (avgGain / avgLoss)))
    }

     fun atr(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int): Double {
        if (closes.size < period + 1) return 1.0
        val trs = ArrayList<Double>(closes.size - 1)
        for (i in 1 until closes.size) trs.add(max(highs[i] - lows[i], max(abs(highs[i] - closes[i - 1]), abs(lows[i] - closes[i - 1]))))
        var atrVal = trs.take(period).average()
        for (i in period until trs.size) atrVal = (atrVal * (period - 1) + trs[i]) / period
        return atrVal
    }
}
        
