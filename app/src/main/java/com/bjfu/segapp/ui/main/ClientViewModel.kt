package com.bjfu.segapp.ui.main

import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bjfu.segapp.MyApplication
import com.bjfu.segapp.databinding.FragmentClientBinding
import com.bjfu.segapp.ui.main.models.ClientModel
import java.io.File

class ClientViewModel : ViewModel() {
    /*调试前缀*/
    var TAG: String = "TTZZ"

    val model: ClientModel = ClientModel()

    /*请求服务器参数*/
    /*本次请求属于的id值*/
    var idNum: MutableLiveData<Int> = MutableLiveData(0)

    /*post上传用户选择img的路径*/
    var imgPath: MutableLiveData<String> = MutableLiveData("")
    var imgHasPosted: MutableLiveData<Boolean> = MutableLiveData(false) // 用来表示是否已经上传完毕img文件

    /*post上传用户label的路径*/
    var labelPath: MutableLiveData<String> = MutableLiveData("")
    var labelHasPosted: MutableLiveData<Boolean> = MutableLiveData(false) // 用来表示是否已经上传完毕label文件


    /**
     * 请求服务器函数。提交用户上传的图片以及地理信息到服务器。
     * @param v View
     * @param binding FragmentClientBinding 可以拿到前端的数据
     */
    fun postImgAndLocationInfo(v: View, binding: FragmentClientBinding, activity: FragmentActivity) {
        Thread {

            val locationViewModel = ViewModelProvider(activity)[LocationViewModel::class.java]
            val segViewModel = ViewModelProvider(activity)[SegViewModel::class.java]

            // 使用retrofit发送get请求
            val idNumData = model.getIdNum() // temp del
//            val idNumData = 1 // temp add
            idNum.postValue(idNumData)
            /*首先保存beforeIv控件中的图片到手机中*/
            val filePathData = model.saveImg(segViewModel.originBitMap.value!!)
            imgPath.postValue(filePathData)


            val paramsMap = HashMap<String, String>()
//            model.getWeatherParams(paramsMap)
//            model.getLocationParams(paramsMap)
            paramsMap["idNum"] = idNumData.toString()
//            paramsMap["latitude"] = "116.348794"
//            paramsMap["longitude"] = "40.002874"
//            paramsMap["location"] = "北京林业大学"
//            paramsMap["weather"] = "晴"
//            paramsMap["temperature"] = "11"
//            paramsMap["windForce"] = "1"
//            paramsMap["windDirection"] = "东风"
//            paramsMap["pm2p5"] = "20"
            paramsMap["latitude"] = locationViewModel.latitude.value ?: ""
            paramsMap["longitude"] = locationViewModel.longitude.value ?: ""
            paramsMap["location"] = locationViewModel.poi.value ?: ""
            paramsMap["weather"] = locationViewModel.weather_tv.value ?: ""
            paramsMap["temperature"] = locationViewModel.temp_tv.value ?: ""
            paramsMap["windForce"] = locationViewModel.windForce_tv.value ?: ""
            paramsMap["windDirection"] = locationViewModel.windDirection_tv.value ?: ""
            paramsMap["pm2p5"] = locationViewModel.pm2p5_tv.value ?: ""
            paramsMap["aqi"] = locationViewModel.aqi_tv.value ?: ""

            for (values in paramsMap){
                println(values)
            }
//            Thread.sleep(2000) // temp add
            model.postImg(File(filePathData), paramsMap) // temp del
            imgHasPosted.postValue(true)
            model.deleteFile(filePathData) // temp del
            /*法一：必须使用Looper函数环绕才可以在子线程中调用Toast。不然会报错。但会造成资源浪费。*/
//            Looper.prepare()
//            model.beginToast(imgPath.value!!)
//            Looper.loop()
            /*法二：直接在activity中添加观察者，让activity自己去Toast*/
            // 主线程中可以用setValue或者postValue，但子线程中只能用postValue！！！
            // 先设置true，再false则为一个上升沿和一个下降沿
            imgHasPosted.postValue(false) // temp del
        }.start()
    }


    /**
     * 请求服务器函数。提交标记文件到服务器。
     */
//    private fun postAfterImg(v: View, binding: FragmentClientBinding) {
//        Thread {
////            labelPath.value = saveImg(binding.beforeIv.drawable.toBitmap())
//            val file = File(labelPath.value!!)
//            val requestFile: RequestBody =
//                file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
//            val part: MultipartBody.Part =
//                MultipartBody.Part.createFormData("userLabel", file.name, requestFile)
//            val partMap: MutableMap<String, RequestBody> = HashMap<String, RequestBody>()
//            partMap["idNum"] = idNum.toString().toRequestBody("text/plain".toMediaTypeOrNull())
//            val call: Call<ResponseBody> = retrofitService.uploadLabel(part, partMap)
//            Log.e(
//                TAG,
//                "post after img response: ${
//                    call.execute().body()?.string() ?: "response from postAfterImg : null!"
//                }"
//            )
//            Looper.prepare()
//            Toast.makeText(MyApplication.context, "${labelPath.value}文件上传成功！", Toast.LENGTH_SHORT)
//                .show()
//            ScannerUtil.deleteSuccess(MyApplication.context, labelPath.value!!)
//            Looper.loop()
//        }.start()
//    }

    /**
     * 请求服务器函数。生成上传文件成功的Toast信息。
     * @param filePath String 文件路径
     * @param code Int 控制显示时间，默认为short
     */
    fun beginToast(filePath: String, code: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(MyApplication.context, "${filePath}文件上传成功！", code).show()
    }
}