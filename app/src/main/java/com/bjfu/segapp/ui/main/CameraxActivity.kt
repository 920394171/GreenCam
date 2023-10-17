package com.bjfu.segapp.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewStub
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bjfu.segapp.MainActivity
import com.bjfu.segapp.R
import com.bjfu.segapp.ui.main.services.OverlayService
import com.bjfu.segapp.ui.main.utils.PrePostProcessor
import com.bjfu.segapp.ui.main.utils.Result
import com.bjfu.segapp.ui.main.utils.ResultView
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraxActivity : AppCompatActivity() {

    private var mModule: Module? = null
    private var mLastAnalysisResultTime: Long = 0
    private var imageCapture: ImageCapture? = null
    private var mResultView: ResultView? = null
    private lateinit var mBackgroundExecutor: ExecutorService

    private lateinit var shiftBtn: Button
    private lateinit var areaTextView: TextView
    var testBeforeCaptureFlag = true

    lateinit var displayMetrics: DisplayMetrics
    var screenWidth = 1
    var screenHeight = 1
    var hasSaved = false
    var firstIn = true

    companion object {
        const val TAG = "TTZZ"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val REQUEST_CODE_PERMISSIONS = 10
        const val GET_PICTURE_RESPONSE_CODE = 222
        val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private var mAllPixArea: Long = 1L

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camerax)

        displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        mAllPixArea = 1L * screenWidth * screenHeight

        // 请求overlay权限
        requestOverlayPermission()

        // 请求相机权限
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        startCamera()

        mBackgroundExecutor = Executors.newSingleThreadExecutor()
        try {
            val path = assets.toString()
            Log.d(TAG, "Working directory path: $path")




            mModule = LiteModuleLoader.load(assetFilePath(applicationContext, "yolov5s.torchscript.ptl"))
            val br = BufferedReader(InputStreamReader(assets.open("classes.txt")))
            var line: String
            val classes: MutableList<String> = ArrayList()
            val inputStream: InputStream = assets.open("classes.txt")
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val text = StringBuilder()
            bufferedReader.forEachLine { classes.add(it) }
            PrePostProcessor.mClasses = Array(classes.size) { classes[it] }
//            classes.toArray(PrePostProcessor.mClasses)
        } catch (e: IOException) {
            Log.e("Object Detection", "Error reading assets", e)
            finish()
        }

        shiftBtn = findViewById<Button>(R.id.shift_btn)
        areaTextView = findViewById<TextView>(R.id.areaTextView)
        areaTextView.text = "0"
        shiftBtn.text = "转换"
        shiftBtn.setOnClickListener {
            firstIn = false
            if (testBeforeCaptureFlag) {
                testBeforeCaptureFlag = !testBeforeCaptureFlag
                stopService(Intent(this, OverlayService::class.java))
                mResultView?.visibility = View.INVISIBLE
                areaTextView.visibility = View.INVISIBLE
                shiftBtn.text = "拍照"
                startCamera(true)
            } else {
//                mResultView?.visibility = View.VISIBLE
//                areaTextView.visibility = View.VISIBLE
//                shiftBtn.text = "转换"
//                startCamera()
                takePhoto()
            }
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
//                    super.onCaptureSuccess(image)
                    @SuppressLint("UnsafeOptInUsageError", "UnsafeExperimentalUsageError") var bitmap: Bitmap = imgToBitmap(image.image!!)
                    // 无论前后摄像头，统一旋转rotate
                    val matrix = Matrix()
                    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                    // 在这里把bitmap设置到页面上
                    val intent = Intent(this@CameraxActivity, MainActivity::class.java)
                    intent.putExtra("bitmap", bitmap)
                    setResult(GET_PICTURE_RESPONSE_CODE, intent)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                }
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @Throws(ExecutionException::class, InterruptedException::class)
    private fun startCamera(changeToBack: Boolean = false) {
        if (!changeToBack) {
            // 启动OverlayService
            if (Settings.canDrawOverlays(this)) {

                // 开启 OverlayService
                startService(Intent(this, OverlayService::class.java))
            } else {

                // 悬浮窗权限未被授予，无法开启 OverlayService
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        imageCapture = ImageCapture.Builder()
            .build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            // Used to bind the lifecycle of cameras to the lifecycle owner
            var cameraProvider: ProcessCameraProvider? = null
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            val textureView: PreviewView = getCameraPreviewTextureView()

//                final Preview preview = new Preview(previewConfig);
            val preview = Preview.Builder().build()

//                preview.setOnPreviewOutputUpdateListener(output -> textureView.setSurfaceTexture(output.getSurfaceTexture()));
            preview.setSurfaceProvider(textureView.surfaceProvider)

            // 默认选择前置摄像头
            val cameraSelector = if (changeToBack) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA // 测试使用后置摄像头
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            // 如果是前摄，则需要设置分析器
            if (!changeToBack)
                imageAnalyzer.setAnalyzer(mBackgroundExecutor) { image: ImageProxy ->
                    // 在这里实现图像分析逻辑
                    val rotationDegrees = image.imageInfo.rotationDegrees
                    if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime >= 300) {
                        val result: AnalysisResult? = analyzeImage(image, rotationDegrees)
//                        val result: AnalysisResult? = analyzeImage(image, rotationDegrees, !changeToBack) // 测试使用后置摄像头
                        Log.e(TAG, "RESULT: $result")
                        mLastAnalysisResultTime = SystemClock.elapsedRealtime()
                        if (result != null) {
                            runOnUiThread {
                                applyToUiAnalyzeImageResult(result)
                            }
                        } else {
                            Log.e(TAG, "RESULT: NULL!!!!!!")
                        }
                    }
                    image.close()
                }
            try {
                // Unbind use cases before rebinding
                cameraProvider!!.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeImage(image: ImageProxy, rotationDegrees: Int, changeToBack: Boolean = false): AnalysisResult? {
        try {
            if (mModule == null) {
                mModule = LiteModuleLoader.load(assetFilePath(applicationContext, "yolov5s.torchscript.ptl"))
            }
        } catch (e: IOException) {
            Log.e("Object Detection", "Error reading assets", e)
            return null
        }
        @SuppressLint("UnsafeOptInUsageError", "UnsafeExperimentalUsageError") var bitmap: Bitmap = imgToBitmap(image.image!!)

        // 无论前后摄像头，统一旋转rotate
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (!changeToBack) {
            // 创建一个新的 Matrix 对象
            val matrixMirror = Matrix()
            // 设置水平镜像翻转
            matrixMirror.preScale(-1f, 1f)
            // 使用 Matrix 对象的 postTranslate() 方法让镜像图片移动回原来的位置
            matrixMirror.postTranslate(bitmap.width.toFloat(), 0f)
            // 创建一个新的 Bitmap 对象
            bitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrixMirror,
                true
            )
        }
        // 如果需要，可以保存图片查看检测情况
//        saveBitmap(bitmap)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB)
        val outputTuple: Array<IValue> = mModule!!.forward(IValue.from(inputTensor)).toTuple()
        val outputTensor = outputTuple[0].toTensor()
        val outputs = outputTensor.dataAsFloatArray
        val imgScaleX = bitmap.width.toFloat() / PrePostProcessor.mInputWidth
        val imgScaleY = bitmap.height.toFloat() / PrePostProcessor.mInputHeight
        val ivScaleX = mResultView!!.width.toFloat() / bitmap.width
        val ivScaleY = mResultView!!.height.toFloat() / bitmap.height
        val results = PrePostProcessor.outputsToNMSPredictions(outputs, imgScaleX, imgScaleY, ivScaleX, ivScaleY, 0f, 0f)
        return AnalysisResult(results)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun saveBitmap(bitmap: Bitmap) {
        val fileName = "myImage.png" // 要保存的文件名
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
        }
        val resolver = this.contentResolver
        var imageUri: Uri? = null
        var outputStream: OutputStream? = null
        try {
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            imageUri = resolver.insert(collection, contentValues)
            outputStream = imageUri?.let { resolver.openOutputStream(it) }

            // 把bitmap压缩进outputStream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream?.flush()
        } catch (e: Exception) {
            resolver.delete(imageUri!!, null, null)
            e.printStackTrace()
        } finally {
            outputStream?.close()
        }


    }


    @Throws(IOException::class)
    open fun assetFilePath(context: Context, assetName: String?): String? {
        val file = File(context.filesDir, assetName!!)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

    private fun imgToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    @SuppressLint("SetTextI18n")
    private fun applyToUiAnalyzeImageResult(result: AnalysisResult) {
        mResultView!!.setResults(result.mResults)

        areaTextView.text = "%.2f".format((mResultView!!.mPersonPixArea * 1.0 / mAllPixArea * 1.0) * 100) + "%"
        mResultView!!.invalidate()
    }

    private fun getCameraPreviewTextureView(): PreviewView {
        mResultView = findViewById(R.id.resultView)
        return if (!firstIn)
            findViewById(R.id.object_detection_texture_view)
        else
            (findViewById<ViewStub>(R.id.object_detection_texture_view_stub))
                .inflate()
                .findViewById(R.id.object_detection_texture_view)
    }

    /**
     * 请求overlay权限
     */
    private fun requestOverlayPermission() {
        Log.e(TAG, "requestOverlayPermission: $packageName")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopService(Intent(this, OverlayService::class.java))
    }

    override fun onResume() {
        super.onResume()
        startService(Intent(this, OverlayService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        mBackgroundExecutor.shutdown()
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//        stopService(Intent(this, OverlayService::class.java))
////        finish()
//    }

    class AnalysisResult(val mResults: ArrayList<Result>)
}