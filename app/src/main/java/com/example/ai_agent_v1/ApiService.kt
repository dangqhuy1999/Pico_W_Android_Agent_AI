package com.example.ai_agent_v1

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

// Lớp này sẽ xử lý tất cả các yêu cầu API
class ApiService {
    private val client = OkHttpClient()

    // Các URL API (thay thế bằng API của bạn)
    private val SPEECH_TO_TEXT_API = "https://api.speechtotext.com/v1/recognize"
    private val AI_AGENT_API = "https://api.aiagent.com/v1/query"
    private val TEXT_TO_SPEECH_API = "https://api.texttospeech.com/v1/synthesize"
    private val PICO_W_IP = "192.168.1.123"
    private val PICO_W_URL = "http://$PICO_W_IP/upload"

    // Gửi âm thanh đến API Speech-to-Text và trả về văn bản
    fun transcribeAudio(file: File): String {
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("audio/mpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder().url(SPEECH_TO_TEXT_API).post(requestBody).build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val json = JSONObject(
                    response.body?.string()
                        ?: """{"text": "Xin chào, tôi có thể giúp gì cho bạn?"}"""
                )
                return json.getString("text")
            } else {
                throw IOException("Lỗi Speech-to-Text API")
            }
        }
    }

    // Gửi văn bản tới AI Agent và trả về câu trả lời
    fun getAiResponse(text: String): String {
        val json = JSONObject().put("prompt", text)
        val requestBody =
            json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(AI_AGENT_API).post(requestBody).build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val json = JSONObject(
                    response.body?.string()
                        ?: """{"answer": "Bạn khỏe không? Tôi rất vui được nói chuyện với bạn."}"""
                )
                return json.getString("answer")
            } else {
                throw IOException("Lỗi AI Agent API")
            }
        }
    }

    // Gửi văn bản tới API Text-to-Speech và trả về dữ liệu âm thanh dưới dạng ByteArray
    fun getTtsAudio(text: String): ByteArray {
        val json = JSONObject().put("text", text)
        val requestBody =
            json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(TEXT_TO_SPEECH_API).post(requestBody).build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return response.body?.bytes()
                    ?: throw IOException("Không nhận được dữ liệu âm thanh từ TTS API")
            } else {
                throw IOException("Lỗi Text-to-Speech API")
            }
        }
    }

    // Gửi dữ liệu âm thanh đã hoàn chỉnh tới Pico W
    fun sendAudioToPicoW(audioData: ByteArray) {
        val requestBody =
            audioData.toRequestBody("audio/mpeg".toMediaTypeOrNull(), 0, audioData.size)
        val request = Request.Builder().url(PICO_W_URL).post(requestBody).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Lỗi khi gửi dữ liệu tới Pico W: ${response.code}")
            }
        }
    }
}
