package com.bjfu.segapp.ui.main.models

import android.graphics.Bitmap
import android.util.Log
import com.bjfu.segapp.MyApplication
import com.bjfu.segapp.ui.main.services.RetrofitService
import com.bjfu.segapp.ui.main.utils.ScannerUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import java.io.File
import java.net.InetAddress

class ClientModel {
    /*调试头*/
    private val TAG: String = "TTZZ"

    /*连接服务器域名*/
//    private var retrofit: Retrofit = Retrofit.Builder().baseUrl("http://116.63.227.32:9999/").build()  // 公网ip
    private var retrofit: Retrofit = Retrofit.Builder().baseUrl("http://172.26.204.62:10000/").build()  // 本地ip，需要调用ipconfig手动更新

    /*service实例，用来向服务器通信的接口*/
    private var retrofitService: RetrofitService = retrofit.create(RetrofitService::class.java)

    /**
     * 请求服务器函数。得到被分配的id。
     * @return Int 被分配的id
     */
    fun getIdNum(): Int {
        val kw: String = "idNum"
        val call: Call<ResponseBody> = retrofitService.getNum(kw)
        val responseBody = call.execute().body()?.string()
        Log.e(TAG, "getIdNum response: ${responseBody ?: "response from getIdNum : null!"}")
        /*要先请求服务器拿到idNum，即数据表中的id字段，之后再插入*/
        return responseBody?.toInt() ?: 0
    }

    /**
     * 请求服务器函数。保存bitmap成文件。
     * @param bitmap Bitmap 需要保存的bitmap对象
     * @return String 文件的路径
     */
    fun saveImg(bitmap: Bitmap): String =
        ScannerUtil.saveImageToGallery(MyApplication.context, bitmap, ScannerUtil.ScannerType.RECEIVER, false)

    /**
     * 请求服务器函数。上传img文件以及地理天气信息参数
     * @param file File img文件
     * @param paramMap MutableMap<String, String> 其他地理天气参数
     */
    fun postImg(file: File, paramMap: MutableMap<String, String>) {
        /*设置为multipart模式，可以提交文件*/
        val requestFile: RequestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("userImg", file.name, requestFile)
        val partMap: MutableMap<String, RequestBody> = HashMap<String, RequestBody>()
        partMap["idNum"] = paramMap["idNum"]!!.toRequestBody("text/plain".toMediaTypeOrNull())
        partMap["latitude"] = paramMap["latitude"]!!.toRequestBody("text/plain".toMediaTypeOrNull())
        partMap["longitude"] = paramMap["longitude"]!!.toRequestBody("text/plain".toMediaTypeOrNull())
        partMap["location"] = paramMap["location"]!!.toRequestBody("text/plain".toMediaTypeOrNull())
        partMap["weather"] = paramMap["weather"]!!.toRequestBody("text/plain".toMediaTypeOrNull())
        partMap["temperature"] = paramMap["temperature"]!!.toRequestBody("text/plain".toMediaTypeOrNull())
        partMap["windForce"] = paramMap["windForce"]!!.toRequestBody("text/plain".toMediaTypeOrNull())
        partMap["windDirection"] = paramMap["windDirection"]!!.toRequestBody("text/plain".toMediaTypeOrNull())
        partMap["pm2p5"] = paramMap["pm2p5"]!!.toRequestBody("text/plain".toMediaTypeOrNull())
        partMap["aqi"] = paramMap["aqi"]!!.toRequestBody("text/plain".toMediaTypeOrNull())
        val call = retrofitService.uploadOriImg(part, partMap)
        Log.e(TAG, "post before img response: ${call.execute().body()?.string() ?: "response from postBeforeImg : null!"}")
    }

    /**
     * 得到本机IP地址函数，弃用。
     * @return String 本机IP地址
     */
    private fun getLocalIp(): String {
        val inetAddress: InetAddress = InetAddress.getLocalHost()
        println(inetAddress.hostAddress)
        return inetAddress.hostAddress?.toString() ?: "127.0.0.1"
    }

    /**
     * 请求服务器函数。删除保存的文件。
     * @param filePath String 要删除的文件路径
     */
    fun deleteFile(filePath: String) {
        ScannerUtil.deleteSuccess(MyApplication.context, filePath)
    }
}