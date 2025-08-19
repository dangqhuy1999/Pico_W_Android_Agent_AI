package com.example.ai_agent_v1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ai_agent_v1.ui.theme.AI_AGENT_v1Theme // Thêm dòng import này
import androidx.compose.material.icons.Icons // Cần thêm import này cho Icon
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.shape.CircleShape // Cần thêm import này cho CircleShape

// Lớp MainActivity giữ nguyên
class MainActivity : ComponentActivity() {

    private val audioViewModel: AudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Yêu cầu quyền truy cập micro
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        setContent {
            AI_AGENT_v1Theme {
                // Toàn bộ logic giao diện sẽ nằm trong hàm AssistantScreen
                AssistantScreen(audioViewModel)
            }
        }
    }
}

// Composable để hiển thị màn hình trợ lý
@Composable
fun AssistantScreen(viewModel: AudioViewModel) {
    val context = LocalContext.current

    // Đặt Surface bên trong hàm @Composable
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Lỗi được khắc phục
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Trợ lý Âm thanh",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Text(
                text = viewModel.statusMessage,
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = {
                    if (viewModel.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        context.externalCacheDir?.let { viewModel.startRecording(it) }
                    }
                },
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.isRecording) Color.Red else Color.Blue)
            ) {
                if (viewModel.isRecording) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Dừng",
                        tint = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Bắt đầu",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = if (viewModel.isRecording) "Đang ghi âm..." else "Nhấn để nói",
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
