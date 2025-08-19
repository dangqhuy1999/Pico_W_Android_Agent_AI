package com.example.ai_agent_v1

import android.media.MediaRecorder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ViewModel để quản lý trạng thái và logic của ứng dụng
class AudioViewModel : ViewModel() {

    // Khai báo các biến trạng thái
    var isRecording by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf("Sẵn sàng")
        private set

    // Khai báo các biến nội bộ
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private val apiService = ApiService()

    // Bắt đầu ghi âm
    fun startRecording(cacheDir: File) {
        // Đặt tên file ghi âm
        audioFile = File(cacheDir, "recording.mp3")

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)

            try {
                prepare()
                start()
                isRecording = true
                statusMessage = "Đang ghi âm..."
            } catch (e: IOException) {
                e.printStackTrace()
                statusMessage = "Lỗi khi bắt đầu ghi âm."
                isRecording = false
            }
        }
    }

    // Dừng ghi âm và bắt đầu xử lý
    fun stopRecording() {
        isRecording = false
        statusMessage = "Đang xử lý..."

        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null

        viewModelScope.launch {
            if (audioFile?.exists() == true) {
                try {
                    // Bước 1: Gửi âm thanh đến API Speech-to-Text
                    val transcript = apiService.transcribeAudio(audioFile!!)

                    statusMessage = "Đã nhận văn bản, đang gửi tới AI..."

                    // Bước 2: Gửi văn bản tới AI Agent
                    val aiText = apiService.getAiResponse(transcript)

                    statusMessage = "Đã nhận câu trả lời, đang chuyển thành âm thanh..."

                    // Bước 3: Gửi câu trả lời của AI tới API Text-to-Speech
                    val ttsAudio = apiService.getTtsAudio(aiText)

                    statusMessage = "Đã có file âm thanh, đang gửi tới Pico W..."

                    // Bước 4: Gửi file âm thanh đã hoàn chỉnh tới Pico W
                    apiService.sendAudioToPicoW(ttsAudio)

                    withContext(Dispatchers.Main) {
                        statusMessage = "Hoàn tất! Pico W đang phát âm thanh."
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        e.printStackTrace()
                        statusMessage = "Lỗi trong quá trình xử lý: ${e.message}"
                    }
                }
            }
        }
    }
}