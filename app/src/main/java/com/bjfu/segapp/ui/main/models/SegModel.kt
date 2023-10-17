package com.bjfu.segapp.ui.main.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.provider.Telephony.TextBasedSmsColumns.PERSON
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Tensor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class SegModel {
    private val TAG: String = "TTZZ"

    //    private val CLASSNUM = 21
    val CLASSNUM = 10
//    private val DOG = 12
//    private val PERSON = 15
//    private val SHEEP = 14

    /**
     * 计算最终的颜色，通过判断得分最大值得到对应的颜色
     * @param height Int
     * @param width Int
     * @param scores FloatArray
     * @param intValues IntArray
     */
    fun getColor(height: Int, width: Int, scores: FloatArray, intValues: IntArray, ratio: Array<Double>) {
        for (j in 0 until height) {
            for (k in 0 until width) {
                var maxi = 0 // 分类
                var maxj = 0 // 第j行 得分目前最大
                var maxk = 0 // 第k列 得分目前最大
                var maxnum = -Double.MAX_VALUE
                for (i in 0 until CLASSNUM) { // 对于每个分类
                    val score = scores[i * (width * height) + j * width + k] // 得到每个分类的得分
                    if (score > maxnum) { // 如果是新的最大值
                        maxnum = score.toDouble() // 记录
                        maxi = i
                        maxj = j
                        maxk = k
                    }
                }

                val colors = listOf(
//                    -0x000000, // 黑色
//                    -0x0000ff, // 蓝色
//                    -0x643c14, // 棕色
//                    -0xffd700, // 橙黄
//                    -0xffff00, // 黄色
//                    -0x96c8dc, // 暗蓝
//                    -0x006400, // 暗绿
//                    -0x46781e, // 芥末
//                    -0x009600, // 草绿
//                    -0xc80096  // 紫色

                    0xFF000000, // (0, 0, 0) background 黑色
                    0xFF0000FF, // (0, 0, 255) water 蓝色
                    0xFF643C14, // (100, 60, 20) architecture 棕色
                    0xFFFFD700, // (255, 215, 0) facility 橙黄
                    0xFFFFFF00, // (255, 255, 0) sky 黄色
                    0xFF96C8DC, // (150, 200, 220) flat 暗蓝
                    0xFF006400, // (0, 100, 0) plant_tree 暗绿
                    0xFF46781E, // (70, 120, 30) plant_shrub 浅绿
                    0xFF009600, // (0, 150, 0) plant_lawn 绿色
                    0xFFC80096  // (200, 0, 150) plant_flower 紫色

//                    ,
//                    0xC80096,  // (200, 0, 150)
//                    0x0000FF, // (0, 0, 255)
//                    0x643C14, // (100, 60, 20)
//                    0xFFD700, // (255, 215, 0)
//                    0xFFFF00, // (255, 255, 0)
//                    0x96C8DC, // (150, 200, 220)
//                    0x006400, // (0, 100, 0)
//                    0x46781E, // (70, 120, 30)
//                    0x009600, // (0, 150, 0)
//                    0x000000 // (0, 0, 0)
                )

                if (maxi != 0) {
                    Log.e(TAG, "getColor: ${colors[maxi]}, --- $maxi")
                }

                intValues[maxj * width + maxk] = colors[maxi].toInt()
                ratio[maxi] += 1.0 / (height * width)

//                when (maxi) {
//                    PERSON -> intValues[maxj * width + maxk] = -0x10000
//                    DOG -> intValues[maxj * width + maxk] = -0xff0100
//                    SHEEP -> intValues[maxj * width + maxk] = -0xffff01
//                    else -> intValues[maxj * width + maxk] = -0x1000000
//
//
//                }
            }
        }
    }

    /**
     * 获取图片的旋转信息
     * @param exifInterface ExifInterface
     * @param degree Int
     * @return Int
     */
    fun getRotateMsg(exifInterface: ExifInterface, degree: Int): Int {
        var degree1 = degree
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> degree1 = 90
            ExifInterface.ORIENTATION_ROTATE_180 -> degree1 = 180
            ExifInterface.ORIENTATION_ROTATE_270 -> degree1 = 270
        }
        return degree1
    }

    /**
     *
     * @param degree Int
     * @param returnBm Bitmap?
     * @param mBitmap Bitmap
     * @return Bitmap?
     */
    fun getMatrixAfterRotate(degree: Int, mBitmap: Bitmap): Bitmap? {
        var returnBm1: Bitmap? = null
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm1 = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.width, mBitmap.height, matrix, true)
        } catch (_: OutOfMemoryError) {
        }
        return returnBm1
    }

    /**
     * 得到图片每个像素的分类得分情况
     * @param outTensors Map<String, IValue>
     * @return FloatArray
     */
    fun getScores(outTensors: Map<String, IValue>): FloatArray {
        val outputTensor: Tensor = outTensors["out"]!!.toTensor()
        val scores: FloatArray = outputTensor.dataAsFloatArray
        return scores
    }

    /**
     * 得到图片每个像素的分类得分情况
     * @param outTensors Map<String, IValue>
     * @return FloatArray
     */
    fun getScores(outputTensor: Tensor): FloatArray {
        val scores: FloatArray = outputTensor.dataAsFloatArray
        return scores
    }

    /**
     * 更新像素颜色为intValues
     * @param outputBitmap Bitmap
     * @param intValues IntArray
     */
    fun getTransferredBitmap(mBitmap: Bitmap?, outputBitmap: Bitmap, intValues: IntArray): Bitmap {
        outputBitmap.setPixels(intValues, 0, outputBitmap.width, 0, 0, outputBitmap.width, outputBitmap.height)
        return Bitmap.createScaledBitmap(outputBitmap, mBitmap!!.width, mBitmap.height, true)
    }

    /**
     * 得到新复制的bitmap
     * @param mBitmap Bitmap?
     * @param width Int
     * @param height Int
     * @return Bitmap
     */
    fun getCopyBitmap(mBitmap: Bitmap?, width: Int, height: Int): Bitmap {
        val bmpSegmentation = Bitmap.createScaledBitmap(mBitmap!!, width, height, true)
        return bmpSegmentation.copy(bmpSegmentation.config, true)
    }

    /**
     * 得到旋转后的位图
     * @param path String
     * @param mBitmap Bitmap?
     * @return Bitmap?
     */
    fun getRotateBitmap(path: String, mBitmap: Bitmap?): Bitmap? {
        var degree = 0 //被旋转的角度
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            val exifInterface = ExifInterface(path)
            degree = getRotateMsg(exifInterface, degree)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var returnBm: Bitmap? = null

        // 根据旋转角度，生成旋转矩阵
        returnBm = mBitmap?.let { getMatrixAfterRotate(degree, it) }
        if (returnBm == null) {
            returnBm = mBitmap
        }
        return returnBm
    }

    /**
     * 得到压缩后的bitmap
     * @param mmBitmap Bitmap?
     * @return Bitmap?
     */
    public fun getCompressedMBitmap(mmBitmap: Bitmap?): Bitmap? {
        val baos = ByteArrayOutputStream()
        var mBitmap = mmBitmap
        mBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos) //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        //        int persents = 100;
        val sampleSize = 2
        val options = BitmapFactory.Options()
        options.inSampleSize = sampleSize
        while (baos.toByteArray().size / 1024 > 1024 * 0.8) {  //循环判断如果压缩后图片是否大于0.8MB,大于继续压缩
            baos.reset() //重置baos即清空baos
            //            mBitmap.compress(Bitmap.CompressFormat.JPEG, persents, baos);//这里压缩options%，把压缩后的数据存放到baos中
            mBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val bytes = baos.toByteArray()
            mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            Log.i("info", "图片大小：" + mBitmap!!.byteCount)

            //            persents -= 5;//每次都减少10
        }
        val isBm = ByteArrayInputStream(baos.toByteArray()) //把压缩后的数据baos存放到ByteArrayInputStream中
        return BitmapFactory.decodeStream(isBm, null, null) //把ByteArrayInputStream数据生成图片
    }
}