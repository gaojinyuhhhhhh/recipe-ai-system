package com.recipe.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private suspend fun getCameraProvider(context: Context): ProcessCameraProvider {
    return suspendCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cont.resume(future.get())
        }, ContextCompat.getMainExecutor(context))
    }
}

@Composable
fun CameraScreen(
    onImageCaptured: (String) -> Unit,
    onDismiss: () -> Unit,
    isRecognizing: Boolean = false,
    recognitionError: String? = null,
    onClearError: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // 权限状态
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    
    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            permissionDenied = true
        }
    }
    
    // 进入页面时检查并请求权限
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    when {
        hasCameraPermission -> {
            // 已授权 - 显示相机预览
            CameraPreviewContent(
                onImageCaptured = onImageCaptured,
                onDismiss = onDismiss,
                isRecognizing = isRecognizing,
                recognitionError = recognitionError,
                onClearError = onClearError
            )
        }
        permissionDenied -> {
            // 权限被拒绝 - 显示提示
            PermissionDeniedContent(
                onRetry = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onDismiss = onDismiss
            )
        }
        else -> {
            // 等待权限结果
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * 相机预览内容（已授权后显示）
 */
@Composable
private fun CameraPreviewContent(
    onImageCaptured: (String) -> Unit,
    onDismiss: () -> Unit,
    isRecognizing: Boolean = false,
    recognitionError: String? = null,
    onClearError: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val imageCapture = remember { ImageCapture.Builder().build() }
    val preview = remember { Preview.Builder().build() }
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    var isCapturing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }
    
    val previewView = remember {
        PreviewView(context).apply {
            preview.setSurfaceProvider(surfaceProvider)
        }
    }
    
    LaunchedEffect(Unit) {
        try {
            val cameraProvider = getCameraProvider(context)
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e("Camera", "绑定相机失败", e)
            captureError = "相机初始化失败: ${e.message}"
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // 拍照按钮
        Button(
            onClick = {
                if (!isCapturing) {
                    isCapturing = true
                    captureError = null
                    Log.d("Camera", "开始拍照...")
                    captureImage(
                        context = context,
                        imageCapture = imageCapture,
                        onImageCaptured = { base64 ->
                            Log.d("Camera", "拍照成功，Base64长度: ${base64.length}")
                            isCapturing = false
                            onImageCaptured(base64)
                        },
                        onError = { error ->
                            Log.e("Camera", "拍照失败: $error")
                            isCapturing = false
                            captureError = error
                        }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .size(72.dp),
            shape = CircleShape,
            enabled = !isCapturing,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "拍照识别",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // 错误提示（拍照错误）
        captureError?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 错误提示（识别错误）
        recognitionError?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = onClearError) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // AI识别中遮罩
        if (isRecognizing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                // 半透明背景
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {}
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AI正在识别食材...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请稍候，这可能需要几秒钟",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // 关闭按钮
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, "关闭", tint = Color.White)
        }
    }
}

/**
 * 权限被拒绝时的提示内容
 */
@Composable
private fun PermissionDeniedContent(
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "需要相机权限",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "拍照识别食材需要使用您的相机\n请在系统设置中允许相机权限",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("重新请求权限")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("返回")
            }
        }
    }
}

fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (String) -> Unit,
    onError: (String) -> Unit = {}
) {
    val file = File(context.cacheDir, "captured_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    
    Log.d("Camera", "takePicture 调用，保存到: ${file.absolutePath}")
    
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    Log.d("Camera", "图片已保存，文件大小: ${file.length()} bytes")
                    // 压缩图片避免 OOM 和请求超大
                    val bitmap = decodeSampledBitmap(file.absolutePath, 1024, 1024)
                    if (bitmap == null) {
                        onError("图片解码失败")
                        return
                    }
                    val base64 = bitmapToBase64(bitmap)
                    Log.d("Camera", "Base64长度: ${base64.length}")
                    bitmap.recycle()
                    onImageCaptured(base64)
                } catch (e: Exception) {
                    Log.e("Camera", "图片处理失败", e)
                    onError("图片处理失败: ${e.message}")
                }
            }
            
            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "拍照失败: ${exception.imageCaptureError}", exception)
                onError("拍照失败: ${exception.message}")
            }
        }
    )
}

/**
 * 压缩解码图片，避免 OOM
 */
fun decodeSampledBitmap(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    // 先读取图片尺寸
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(filePath, options)
    
    // 计算采样率
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    options.inJustDecodeBounds = false
    
    return BitmapFactory.decodeFile(filePath, options)
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun bitmapToBase64(bitmap: Bitmap): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}