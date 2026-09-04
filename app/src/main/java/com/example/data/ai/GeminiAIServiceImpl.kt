package com.example.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAIServiceImpl(
    private val context: Context? = null
) : FocusAIService {

    private val tag = "GeminiAIService"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun isOnline(): Boolean {
        if (context == null) return true
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    override fun breakdownTask(taskTitle: String, taskDescription: String): Flow<List<String>> = flow {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }

        if (!isOnline() || apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(tag, "Using offline smart breakdown for task: $taskTitle")
            emit(generateOfflineBreakdown(taskTitle, taskDescription))
            return@flow
        }

        try {
            val prompt = """
                أنت مساعد إنتاجية ذكي لتطبيق FocusCraft.
                قم بتفكيك المهمة التالية إلى 3 إلى 5 خطوات فرعية عملية ومحددة وقابلة للإنجاز الفوري باللغة العربية.
                عنوان المهمة: "$taskTitle"
                ${if (taskDescription.isNotBlank()) "تفاصيل إضافية: \"$taskDescription\"" else ""}
                
                الشروط:
                1. أرجع فقط قائمة الخطوات الفرعية بدون ترقيم وبدون شرطات وبدون عناوين ترحيبية.
                2. سطر واحد لكل خطوة فرعية.
                3. يجب أن تكون كل خطوة واضحة وقصيرة (أقل من 8 كلمات).
            """.trimIndent()

            val responseText = callGeminiApi(prompt, apiKey)
            val subTasks = responseText
                .lines()
                .map { it.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") && !it.contains("الخطوات:", ignoreCase = true) }
                .take(5)

            if (subTasks.isNotEmpty()) {
                emit(subTasks)
            } else {
                emit(generateOfflineBreakdown(taskTitle, taskDescription))
            }
        } catch (e: Exception) {
            Log.w(tag, "Gemini breakdown failed, falling back to offline", e)
            emit(generateOfflineBreakdown(taskTitle, taskDescription))
        }
    }.flowOn(Dispatchers.IO)

    override fun estimateDuration(taskTitle: String): Flow<Int> = flow {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }

        if (!isOnline() || apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            emit(generateOfflineDurationEstimate(taskTitle))
            return@flow
        }

        try {
            val prompt = """
                أنت خبير تقدير أوقات المهام.
                قدر الوقت الواقعي بالدقائق لإنجاز المهمة التالية: "$taskTitle".
                أرجع رقماً صحيحاً فقط يمثل الدقائق (مثل 45 أو 60) بدون أي كلمات أخرى.
            """.trimIndent()

            val responseText = callGeminiApi(prompt, apiKey)
            val digitsOnly = responseText.filter { it.isDigit() }
            val minutes = digitsOnly.toIntOrNull()?.coerceIn(15, 240) ?: generateOfflineDurationEstimate(taskTitle)
            emit(minutes)
        } catch (e: Exception) {
            Log.w(tag, "Gemini estimation failed, using fallback", e)
            emit(generateOfflineDurationEstimate(taskTitle))
        }
    }.flowOn(Dispatchers.IO)

    override fun suggestDeFrictionPlan(taskTitle: String, postponedCount: Int): Flow<String> = flow {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }

        if (!isOnline() || apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            emit(generateOfflineDeFrictionPlan(taskTitle, postponedCount))
            return@flow
        }

        try {
            val prompt = """
                أنت مدرب نفسي وسلوكي في تطبيق FocusCraft.
                المهمة "$taskTitle" تم تأجيلها $postponedCount مرات من قبل المستخدم.
                اكتب نصيحة دافئة وقوية ومحفزة في جملتين فقط باللغة العربية الفصحى لمساعدة المستخدم على كسر حاجز التسويف والبدء فوراً في أول خطوة بسيطة (Micro-step).
                لا تضع علامات اقتباس أو مقدمات.
            """.trimIndent()

            val responseText = callGeminiApi(prompt, apiKey).trim()
            if (responseText.isNotBlank()) {
                emit(responseText)
            } else {
                emit(generateOfflineDeFrictionPlan(taskTitle, postponedCount))
            }
        } catch (e: Exception) {
            Log.w(tag, "Gemini defriction failed, using fallback", e)
            emit(generateOfflineDeFrictionPlan(taskTitle, postponedCount))
        }
    }.flowOn(Dispatchers.IO)

    private fun callGeminiApi(prompt: String, apiKey: String): String {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", prompt)
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)
        }

        val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            throw IllegalStateException("Gemini API call failed with code ${response.code}: $errorBody")
        }

        val responseBodyString = response.body?.string() ?: throw IllegalStateException("Empty response from Gemini API")
        val responseJson = JSONObject(responseBodyString)
        val candidates = responseJson.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val firstPart = parts?.optJSONObject(0)
        return firstPart?.optString("text")?.trim() ?: ""
    }

    private fun generateOfflineBreakdown(title: String, desc: String): List<String> {
        val lower = title.lowercase()
        return when {
            lower.contains("تقرير") || lower.contains("بحث") || lower.contains("كتابة") || lower.contains("مقال") -> listOf(
                "تحديد الهيكل والمراجع الأساسية",
                "كتابة المسودة الأولية بدون تدقيق",
                "مراجعة التدقيق اللغوي والتنسيق النهائي"
            )
            lower.contains("برمج") || lower.contains("كود") || lower.contains("تطبيق") || lower.contains("موقع") -> listOf(
                "تحديد المتطلبات وتصميم الواجهة/الهيكلية",
                "تنفيذ الوظائف البرمجية الأساسية",
                "اختبار الكود وحل الأخطاء البرمجية"
            )
            lower.contains("مذاكرة") || lower.contains("دراسة") || lower.contains("امتحان") || lower.contains("كتاب") -> listOf(
                "قراءة سريعة للعناوين وتحديد الأهداف",
                "التركيز على الفصول الصعبة وتلخيصها",
                "حل تدريبات ومراجعة النقاط المهمة"
            )
            lower.contains("تصميم") || lower.contains("رسم") || lower.contains("تعديل") -> listOf(
                "جمع الأفكار واللوحات الإلهامية",
                "رسم المسودة المبدئية للشكل",
                "إضافة التفاصيل والألوان واللمسات النهائية"
            )
            else -> listOf(
                "تجهيز بيئة العمل والمتطلبات الأساسية",
                "إنجاز المرحلة الأولى من المهمة",
                "المراجعة والتأكد من إتمام العمل"
            )
        }
    }

    private fun generateOfflineDurationEstimate(title: String): Int {
        val lower = title.lowercase()
        return when {
            lower.contains("سريع") || lower.contains("مكالمة") || lower.contains("بريد") -> 20
            lower.contains("مذاكرة") || lower.contains("تقرير") || lower.contains("بحث") -> 60
            lower.contains("برمج") || lower.contains("مشروع") || lower.contains("تصميم") -> 90
            else -> 45
        }
    }

    private fun generateOfflineDeFrictionPlan(title: String, postponedCount: Int): String {
        return "ابدأ بأول 5 دقائق فقط دون التزام بإكمال المهمة كاملة. مجرد تجهيز بيئة العمل والبدء سيكسر حاجز التسويف ويستعيد الزخم."
    }
}
