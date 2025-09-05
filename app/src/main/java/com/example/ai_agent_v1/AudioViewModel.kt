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
import android.util.Log
import android.media.MediaRecorder.OutputFormat
import android.media.MediaRecorder.AudioEncoder

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

    /**
     * Bắt đầu ghi âm và lưu vào bộ nhớ tạm thời của ứng dụng.
     * @param cacheDir Thư mục cache của ứng dụng để lưu file tạm thời.
     */
    fun startRecording(cacheDir: File) {
        // Cập nhật trạng thái giao diện người dùng
        isRecording = true
        statusMessage = "Đang ghi âm..."

        // Đặt tên file ghi âm với định dạng .mp4
        audioFile = File(cacheDir, "recording.mp4")

        // Khởi tạo MediaRecorder và cấu hình
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(OutputFormat.MPEG_4)
            setAudioEncoder(AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)

            try {
                prepare()
                start()
                Log.d("AudioViewModel", "Bắt đầu ghi âm thành công. File: ${audioFile?.absolutePath}")
            } catch (e: IOException) {
                // Xử lý lỗi khi chuẩn bị hoặc bắt đầu ghi âm
                e.printStackTrace()
                statusMessage = "Lỗi IOException khi bắt đầu ghi âm."
                isRecording = false
            } catch (e: IllegalStateException) {
                // Xử lý lỗi khi gọi hàm không đúng trạng thái
                e.printStackTrace()
                statusMessage = "Lỗi IllegalStateException khi bắt đầu ghi âm."
                isRecording = false
            }
        }
    }

    /**
     * Dừng ghi âm và bắt đầu xử lý toàn bộ quy trình.
     * Quá trình này bao gồm: ghi âm -> Speech-to-Text -> Gọi AI -> Text-to-Speech -> Gửi tới Pico W.
     * Tất cả các tác vụ mạng đều được chuyển sang luồng I/O để tránh lỗi NetworkOnMainThreadException.
     */
    fun stopRecording() {
        // Cập nhật trạng thái giao diện người dùng
        isRecording = false
        statusMessage = "Đang xử lý..."

        // Dừng và giải phóng MediaRecorder
        mediaRecorder?.apply {
            try {
                stop()

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("AudioViewModel", "Lỗi khi dừng MediaRecorder: ${e.message}")
            } finally {
                release()
            }
        }
        mediaRecorder = null
        // Khởi chạy một coroutine để thực hiện các tác vụ bất đồng bộ
        viewModelScope.launch {
            if (audioFile?.exists() == true) {
                try {
                    Log.d("TestMain", "Bắt đầu quy trình test phiên dịch end-to-end...")

                    // Bước 1: Gửi âm thanh đến API Speech-to-Text
                    // Chuyển sang luồng I/O để thực hiện tác vụ mạng
                    val transcript = withContext(Dispatchers.IO) {
                        apiService.transcribeAudio(audioFile!!)
                    }
                    Log.d("TestMain", "✅ THÀNH CÔNG: Kết quả phiên dịch là: $transcript")

                    // Cập nhật trạng thái trên luồng chính
                    statusMessage = "Đã nhận văn bản, đang gửi tới AI..."

                     // Bước 2: Gửi văn bản tới AI Agent
                     // Chuyển sang luồng I/O để thực hiện tác vụ mạng
                     val aiText = withContext(Dispatchers.IO) {
                         apiService.getAiResponse(transcript)
                     }
                    Log.d("TestMain", "✅ THÀNH CÔNG: Kết quả AI response là: $aiText")

                    // Cập nhật trạng thái trên luồng chính
                    statusMessage = "Đã nhận câu trả lời, đang chuyển thành âm thanh..."

                    // Bước 3: Gửi câu trả lời của AI tới API Text-to-Speech
                    // Chuyển sang luồng I/O để thực hiện tác vụ mạng

                    statusMessage = "Đang truyền phát âm thanh tới Pico W..."
                    withContext(Dispatchers.IO) {
                        apiService.streamTtsAudioToPicoW(aiText)
                    }
                    // Cập nhật trạng thái hoàn tất trên luồng chính
                    withContext(Dispatchers.Main) {
                        statusMessage = "Hoàn tất! Pico W đã nhận và đang phát âm thanh."
                    }
                } catch (e: Exception) {
                    // Xử lý ngoại lệ và hiển thị thông báo lỗi trên luồng chính
                    withContext(Dispatchers.Main) {
                        e.printStackTrace()
                        statusMessage = "Lỗi trong quá trình xử lý: ${e.message}"
                    }
                } finally {
                    // Đảm bảo file được xóa sau khi xử lý xong (thành công hoặc thất bại)
                    audioFile?.delete()
                    Log.d("AudioViewModel", "File ghi âm đã được dọn dẹp.")
                    audioFile = null // Reset biến để tránh sử dụng lại
                }
            } else {
                Log.e("AudioViewModel", "LỖI: File ghi âm không tồn tại.")
                statusMessage = "Lỗi: File ghi âm không tồn tại."
            }
        }
    }
}
