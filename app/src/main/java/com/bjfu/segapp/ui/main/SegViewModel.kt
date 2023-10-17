package com.bjfu.segapp.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bjfu.segapp.MyApplication
import com.bjfu.segapp.databinding.FragmentSegBinding
import com.bjfu.segapp.ui.main.models.SegModel
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.*
import java.util.*
import kotlin.math.log

class SegViewModel : ViewModel(), Runnable {

    var segHasDone: MutableLiveData<Boolean> = MutableLiveData(false)
    var originBitMap: MutableLiveData<Bitmap> = MutableLiveData()
    lateinit var ratio: Array<Double>

    lateinit var transferredBitmap: Bitmap

    var model: SegModel = SegModel()
    var mImageView: ImageView? = null
    var mButtonSegment: Button? = null
    var mProgressBar: ProgressBar? = null
    var mBitmap: Bitmap? = null
    var mRatios: TextView? = null
    private var mModule: Module? = null
//    private String mImagename = "deeplab.jpg";
    //    private String mImagename = "deeplab.jpg";
    // see http://host.robots.ox.ac.uk:8080/pascal/VOC/voc2007/segexamples/index.html for the list of classes with indexes


    val items = arrayOf(
        "拍照",
        "从相册中选择"
    )

    val TAKE_PHOTO = 1 //声明一个请求码，用于识别返回的结果
    val SCAN_OPEN_PHONE = 2 // 相册

    var imageUri: Uri? = null
    var path: String? = null
    var picpath: String? = null

    fun beginToSeg(binding: FragmentSegBinding) {

//        try {
//            mBitmap = BitmapFactory.decodeStream(getAssets().open(mImagename));
//        } catch (IOException e) {
//            Log.e("ImageSegmentation", "Error reading assets", e);
//            finish();
//        }
        mImageView = binding.imageView
//        mImageView.setImageBitmap(mBitmap);

//        mImageView.setImageBitmap(mBitmap);
        val buttonRestart: Button = binding.restartButton


        mButtonSegment = binding.segmentButton
        mProgressBar = binding.progressBar
        mRatios = binding.ratioText
        mButtonSegment!!.setOnClickListener {
            // mBitmap = ((BitmapDrawable) ((ImageView) mImageView).getDrawable()).getBitmap();
            // 如果点击了则禁用Segment按钮，并显示进度条，开启线程进行分割
            mButtonSegment!!.isEnabled = false
            mProgressBar!!.visibility = ProgressBar.VISIBLE
            mButtonSegment!!.text = "Running the model..."
            val thread: Thread = Thread(this)
            thread.start()
        }

        try {
            // 加载权重文件ptl
//            mModule = LiteModuleLoader.load(assetFilePath(MyApplication.context, "deeplabv3_scripted_optimized.ptl"))
//            mModule = LiteModuleLoader.load(assetFilePath(MyApplication.context, "deeplabv3_scripted_optimized_new.ptl"))
            mModule = LiteModuleLoader.load(assetFilePath(MyApplication.context, "segformer_b1_scripted_optimized(1).ptl"))
        } catch (e: IOException) {
            Log.e("ImageSegmentation", "Error reading assets", e)
        }
    }

    @Throws(IOException::class)
    fun assetFilePath(context: Context, assetName: String): String? {
        val file = File(context.filesDir, assetName)
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

    override fun run() {
        // 先压缩图片质量
        compressByQuality()
        segHasDone.postValue(false)
        // bitmap转成tensor
        val inputTensor: Tensor = TensorImageUtils.bitmapToFloat32Tensor(
            mBitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
//        val inputs: FloatArray = inputTensor.dataAsFloatArray
        // 得到推理时间
        val startTime = SystemClock.elapsedRealtime()
        val iValue: IValue = IValue.from(inputTensor)
        // 运行推理
//        val forward = mModule?.forward(iValue)
//        val outTensors: Map<String, IValue> = forward!!.toDictStringKey()
        val outTensors: Tensor = mModule?.forward(iValue)!!.toTensor()

//        /**
//         * 测试，暂时注释
        val inferenceTime = SystemClock.elapsedRealtime() - startTime
        // 打印推理时间
        Log.d("ImageSegmentation", "inference time (ms): $inferenceTime")
        // 得到图片每个像素的分类得分情况
        val scores: FloatArray = model.getScores(outTensors)
//        Log.e(TAG, "run: ${scores.toString()}" )
        // 得到图片尺寸
        val width: Int = mBitmap?.width ?: 0
        val height: Int = mBitmap?.height ?: 0
        // 得到一个新的一维数组，全部初始化为0，用来记录最终的分类的颜色
        val intValues = IntArray(width * height)
        // 计算最终的颜色，通过判断得分最大值得到对应的颜色
        ratio = Array(model.CLASSNUM) { _ -> 0.0 }
        model.getColor(height, width, scores, intValues, ratio)

        // 得到新复制的bitmap
        val outputBitmap = model.getCopyBitmap(mBitmap, width, height)
        // 更新像素颜色为intValues
        transferredBitmap = model.getTransferredBitmap(mBitmap, outputBitmap, intValues)
        segHasDone.postValue(true)

//        */
    }

    /**
     * 压缩图片质量
     */
    private fun compressByQuality() {
        mBitmap = model.getCompressedMBitmap(mBitmap)
    }


    /**
     * 旋转图片
     * @param path String
     */
    fun resolveRotate(path: String) {
        val returnBm: Bitmap? = model.getRotateBitmap(path, mBitmap)
        mBitmap = returnBm
    }


}