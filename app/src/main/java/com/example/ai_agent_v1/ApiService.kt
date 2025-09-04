package com.example.ai_agent_v1

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import android.util.Log // Import Log để in ra Logcat
import java.net.Socket
import okio.use
import java.io.OutputStream

/**
 * Lớp này xử lý tất cả các yêu cầu API một cách bất đồng bộ
 * bằng cách sử dụng các hàm 'suspend'.
 */
class ApiService {
    // Khai báo OkHttpClient một lần để tái sử dụng
    // SỬA LỖI: Tạo OkHttpClient tùy chỉnh để bỏ qua lỗi chứng chỉ SSL
    private val client: OkHttpClient = try {
        // Tạo một TrustManager chấp nhận tất cả các chứng chỉ
        val trustAllCerts: Array<TrustManager> = arrayOf(
            object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        )

        // Cài đặt TrustManager tùy chỉnh
        val sslContext: SSLContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory

        OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Chấp nhận tất cả các hostname
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    } catch (e: Exception) {
        Log.e("ApiService", "Lỗi khi tạo OkHttpClient an toàn: ${e.message}")
        OkHttpClient.Builder().build() // Quay lại client mặc định nếu có lỗi
    }
    /*
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Tăng thời gian chờ kết nối
        .readTimeout(30, TimeUnit.SECONDS)    // Tăng thời gian chờ đọc dữ liệu
        .writeTimeout(30, TimeUnit.SECONDS)   // Tăng thời gian chờ ghi dữ liệu
        .build()
    */
    // Khai báo các biến API Key và URL
    private val ASSEMBLYAI_API_KEY = BuildConfig.ASSEMBLYAI_API_KEY
    private val ASSEMBLYAI_BASE_URL = BuildConfig.ASSEMBLYAI_BASE_URL
    private val UPLOAD_API = "${ASSEMBLYAI_BASE_URL}${BuildConfig.UPLOAD_API}"
    private val TRANSCRIPT_API = "${ASSEMBLYAI_BASE_URL}${BuildConfig.TRANSCRIPT_API}"

    private val AI_AGENT_API = BuildConfig.AI_AGENT_API
    private val TEXT_TO_SPEECH_API = BuildConfig.TEXT_TO_SPEECH_API
    private val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY

    // private val PICO_W_URL = BuildConfig.PICO_W_URL
    private val PICO_W_HOST = BuildConfig.PICO_W_HOST
    private val PICO_W_PORT = BuildConfig.PICO_W_PORT



    /**
     * Gửi âm thanh đến API Speech-to-Text của AssemblyAI và trả về văn bản đã được phiên dịch.
     * Đây là một hàm 'suspend' tổng hợp, xử lý toàn bộ quy trình 3 bước.
     */
    suspend fun transcribeAudio(file: File): String {
        try {
            // Bước 1: Tải file lên và lấy URL tạm thời
            Log.d("ApiServiceTest", "Bước 1: Tải file ${file.name}...")
            val audioUrl = uploadAudio(file)
            Log.d("ApiServiceTest", "Tải lên thành công. URL tạm thời: $audioUrl")

            // Bước 2: Gửi URL để yêu cầu phiên dịch
            Log.d("ApiServiceTest", "Bước 2: Gửi yêu cầu phiên dịch...")
            val transcriptId = submitForTranscription(audioUrl)
            Log.d("ApiServiceTest", "Yêu cầu đã được gửi. ID phiên dịch: $transcriptId")

            // Bước 3: Lặp đi lặp lại để kiểm tra kết quả
            Log.d("ApiServiceTest", "Bước 3: Bắt đầu kiểm tra trạng thái (polling)...")
            return pollForTranscriptionResult(transcriptId)
        } catch (e: Exception) {
            Log.e("ApiServiceTest", "Lỗi Speech-to-Text API: ${e.message}", e)
            throw IOException("Lỗi Speech-to-Text API: ${e.message}", e)
        }
    }

    /**
     * Tải file âm thanh lên AssemblyAI và trả về URL của file.
     * Sử dụng suspendCoroutine để đợi phản hồi bất đồng bộ.
     */
    private suspend fun uploadAudio(file: File): String = suspendCoroutine { continuation ->
        val requestBody = file.asRequestBody("audio/mpeg".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(UPLOAD_API)
            .header("authorization", ASSEMBLYAI_API_KEY)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "{}")
                        val uploadUrl = json.getString("upload_url")
                        continuation.resume(uploadUrl)
                    } else {
                        continuation.resumeWithException(IOException("Lỗi khi tải file: ${response.code}"))
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }

    /**
     * Gửi URL âm thanh đến AssemblyAI để yêu cầu phiên dịch
     * và trả về ID của phiên dịch đó.
     */
    private suspend fun submitForTranscription(audioUrl: String): String = suspendCoroutine { continuation ->
        val json = JSONObject().put("audio_url", audioUrl)
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(TRANSCRIPT_API)
            .header("authorization", ASSEMBLYAI_API_KEY)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "{}")
                        val transcriptId = json.getString("id")
                        continuation.resume(transcriptId)
                    } else {
                        continuation.resumeWithException(IOException("Lỗi khi gửi yêu cầu phiên dịch: ${response.code}"))
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }

    /**
     * Lặp đi lặp lại để kiểm tra trạng thái phiên dịch
     * cho đến khi có kết quả hoàn chỉnh hoặc gặp lỗi.
     */
    private suspend fun pollForTranscriptionResult(transcriptId: String): String {
        val pollingEndpoint = "$TRANSCRIPT_API/$transcriptId"
        while (true) {
            val request = Request.Builder()
                .url(pollingEndpoint)
                .header("authorization", ASSEMBLYAI_API_KEY)
                .get()
                .build()

            // Coroutine sẽ tạm dừng ở đây cho đến khi có phản hồi
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Lỗi khi kiểm tra kết quả: ${response.code}")
                }
                val json = JSONObject(response.body?.string() ?: "{}")
                val status = json.getString("status")

                Log.d("ApiServiceTest", "Trạng thái hiện tại: $status")

                if (status == "completed") {
                    val transcriptText = json.getString("text")
                    return transcriptText
                } else if (status == "error") {
                    throw IOException("Phiên dịch thất bại: ${json.optString("error")}")
                }
            }
            // Dừng 3 giây trước khi kiểm tra lại để tránh quá tải API
            delay(3000)
        }
    }

    /**
     * Gửi văn bản tới AI Agent và trả về câu trả lời.
     * Hàm này cũng là một suspend function để chạy bất đồng bộ.
     */

    suspend fun getAiResponse(
        messageContent: String
    ): String {

        try {
            // 1. Tạo JSON body khớp với cấu trúc API của bạn
            val json = JSONObject().apply {
                put("message_content", messageContent)
                put("user_name", "Lance")
                put("thread_id", "123")
            }
            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            // 2. Tạo Request với URL ngrok và phương thức POST
            val request = Request.Builder()
                .url(AI_AGENT_API) // Sử dụng URL ngrok
                .post(requestBody)
                .build()

            // 3. Thực thi yêu cầu đồng bộ (hoặc bất đồng bộ nếu bạn muốn)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    // Đọc toàn bộ phản hồi từ server
                    val responseBody = response.body?.string() ?: """{"message": "Không có phản hồi"}"""
                    Log.d("ApiService", "✅ Full Response Body: $responseBody")

                    val jsonResponse = JSONObject(responseBody)
                    // SỬA LỖI: Sử dụng optString với tên trường "response"
                    val answer = jsonResponse.optString("response", "Xin lỗi, tôi không có câu trả lời.")

                    // Ghi log thành công
                    Log.d("ApiService", "✅ API Call SUCCESS: Response received")
                    Log.d("ApiService", "✅ Answer: $answer")

                    return answer
                } else {
                    // Ghi log lỗi
                    Log.e("ApiService", "❌ API Call FAILED: HTTP code ${response.code}")
                    Log.e("ApiService", "❌ Error body: ${response.body?.string()}")
                    throw IOException("Lỗi AI Agent API: ${response.code}")
                }
            }
        } catch (e: Exception) {
            // Ghi log ngoại lệ
            Log.e("ApiService", "❌ API Call FAILED: Exception occurred", e)
            throw e
        }
    }


    // Cả hai hàm đều cần chạy bất đồng bộ, nên chúng ta sẽ chuyển logic sang một luồng riêng
// và gọi chúng từ AudioViewModel

    /**
     * Gửi dữ liệu âm thanh theo luồng tới Pico W.
     * Hàm này sẽ nhận các gói dữ liệu (bytes) và gửi chúng ngay lập tức.
     */
    suspend fun sendAudioStreamToPicoW(audioDataChunk: ByteArray) {
        val requestBody = audioDataChunk.toRequestBody("audio/mpeg".toMediaTypeOrNull(), 0, audioDataChunk.size)
        val request = Request.Builder()
            .url("TEST")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Lỗi khi gửi dữ liệu tới Pico W: ${response.code}")
            }
        }
    }

    /**
     * Gửi yêu cầu tới API TTS và xử lý phản hồi theo luồng.
     * Nó sẽ đọc từng gói dữ liệu nhỏ và gửi ngay lập tức đến Pico W.

    suspend fun streamTtsAudioToPicoW_(text: String) {
        val json = JSONObject().apply {
            put("model", "gpt-4o-mini-tts")
            put("input", text)
            put("voice", "coral")
            put("instructions", "Speak in a cheerful and positive tone.")
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(TEXT_TO_SPEECH_API)
            .header("Authorization", "Bearer $OPENAI_API_KEY")
            .post(requestBody)
            .build()

        // Sử dụng newCall().execute() để có được một luồng phản hồi
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Lỗi Text-to-Speech API: ${response.code} - ${response.body?.string()}")
            }

            // Đọc dữ liệu từ luồng (stream) phản hồi
            val source = response.body?.source() ?: throw IOException("Không nhận được dữ liệu âm thanh từ TTS API")
            val buffer = source.buffer()
            val bufferSize = 4096 // Kích thước mỗi gói dữ liệu (chunk)

            while (!buffer.exhausted()) {
                val chunk = ByteArray(bufferSize)
                val bytesRead = buffer.read(chunk, 0, chunk.size)
                if (bytesRead > 0) {
                    // Gửi ngay lập tức gói dữ liệu nhỏ này tới Pico W
                    sendAudioStreamToPicoW(chunk.copyOf(bytesRead))
                }
            }
        }
    }

    suspend fun streamTtsAudioToPicoW(text: String) {
        // Bước 1: Gọi API TTS để nhận luồng dữ liệu
        val json = JSONObject().apply {
            put("model", "gpt-4o-mini-tts")
            put("input", text)
            put("voice", "coral")
            put("instructions", "Speak in a cheerful and positive tone.")
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(TEXT_TO_SPEECH_API)
            .header("Authorization", "Bearer $OPENAI_API_KEY")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { ttsResponse ->
            if (!ttsResponse.isSuccessful) {
                throw IOException("Lỗi Text-to-Speech API: ${ttsResponse.code} - ${ttsResponse.body?.string()}")
            }

            // Lấy luồng dữ liệu thô từ TTS API
            val ttsInputStream = ttsResponse.body?.byteStream() ?: throw IOException("Không nhận được dữ liệu âm thanh từ TTS API")

            // Bước 2: Mở một kết nối TCP duy nhất đến Pico W
            try {
                val portAsInt = PICO_W_PORT.toInt()

                Socket(PICO_W_HOST, portAsInt).use { socket ->
                    println("Đã kết nối thành công tới Pico W TCP Server.")
                    val outputStream: OutputStream = socket.getOutputStream()

                    // Bước 3: Đọc dữ liệu từ luồng TTS và ghi trực tiếp vào socket
                    val bufferSize = 4096 // Kích thước mỗi gói dữ liệu (chunk)
                    val chunk = ByteArray(bufferSize)
                    var bytesRead: Int

                    while (ttsInputStream.read(chunk).also { bytesRead = it } != -1) {
                        if (bytesRead > 0) {
                            outputStream.write(chunk, 0, bytesRead)
                            outputStream.flush() // Quan trọng: Đẩy dữ liệu đi ngay lập tức
                        }
                    }

                    outputStream.flush()
                    println("Hoàn tất việc truyền dữ liệu âm thanh tới Pico W.")
                } // Khối 'use' tự động đóng socket
            } catch (e: Exception) {
                throw IOException("Lỗi khi truyền dữ liệu tới Pico W: ${e.message}")
            } finally {
                // Đảm bảo luồng đầu vào từ TTS API cũng được đóng
                ttsInputStream.close()
            }
        }
    }
     */


    suspend fun streamTtsAudioToPicoW(text: String) = withContext(Dispatchers.IO) {
        var ttsTempFile: File? = null
        var decodedTempFile: File? = null

        try {
            // Bước 1: Gọi API TTS và lưu dữ liệu vào tệp tạm thời
            ttsTempFile = File.createTempFile("tts_audio_input", ".mp3")
            val json = JSONObject().apply {
                put("model", "gpt-4o-mini-tts")
                put("input", text)
                put("voice", "coral")
                put("instructions", "Speak in a cheerful and positive tone.")
            }
            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(TEXT_TO_SPEECH_API)
                .header("Authorization", "Bearer $OPENAI_API_KEY")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { ttsResponse ->
                if (!ttsResponse.isSuccessful) {
                    val errorBody = ttsResponse.body?.string() ?: "Unknown error"
                    throw IOException("Lỗi TTS API: ${ttsResponse.code} - $errorBody")
                }
                ttsResponse.body?.byteStream()?.use { input ->
                    ttsTempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("Không nhận được dữ liệu âm thanh")
            }

            // BƯỚC SỬA LỖI: Thêm -y để ghi đè file đã có
            // Bước 2: Chạy FFmpegKit để giải mã từ tệp tạm thời và lưu vào một tệp tạm thời khác
            decodedTempFile = File.createTempFile("tts_audio_output", ".wav")
            val arguments = arrayOf("-y", "-i", ttsTempFile.absolutePath, "-f", "s16le", "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2", decodedTempFile.absolutePath)
            val session = FFmpegKit.executeWithArguments(arguments)

            // Bước 3: Kết nối TCP và gửi dữ liệu từ tệp đã giải mã
            if (ReturnCode.isSuccess(session.returnCode)) {
                val portAsInt = PICO_W_PORT.toInt()
                Socket(PICO_W_HOST, portAsInt).use { socket ->
                    Log.d("PicoTTSApp", "Đã kết nối thành công tới Pico W TCP Server.")
                    val outputStream: OutputStream = socket.getOutputStream()
                    decodedTempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    Log.d("PicoTTSApp", "Hoàn tất việc truyền dữ liệu âm thanh.")
                }
            } else {
                val allLogs = session.getLogsAsString()
                Log.e("PicoTTSApp", "Lỗi FFmpeg: ${session.failStackTrace}")
                Log.e("PicoTTSApp", "Chi tiết Log FFmpeg: $allLogs")
            }
        } catch (e: Exception) {
            Log.e("PicoTTSApp", "Lỗi khi truyền dữ liệu: ${e.message}", e)
        } finally {
            ttsTempFile?.delete()
            decodedTempFile?.delete()
        }
    }
}