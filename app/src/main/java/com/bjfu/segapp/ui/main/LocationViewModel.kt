package com.bjfu.segapp.ui.main

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import com.baidu.location.*
import com.bjfu.segapp.MyApplication
import com.bjfu.segapp.databinding.FragmentLocationBinding
import com.bjfu.segapp.ui.main.models.LocationModel
import com.google.gson.Gson
import com.qweather.sdk.bean.air.AirNowBean
import com.qweather.sdk.bean.base.Code
import com.qweather.sdk.bean.base.Lang
import com.qweather.sdk.bean.base.Unit
import com.qweather.sdk.bean.weather.WeatherNowBean
import com.qweather.sdk.view.HeConfig
import com.qweather.sdk.view.QWeather
import com.qweather.sdk.view.QWeather.OnResultAirNowListener
import com.qweather.sdk.view.QWeather.OnResultWeatherNowListener

class LocationViewModel : ViewModel() {

    private val TAG: String = "TTZZ"

    private var mLocationClient: LocationClient? = null
    private val myListener: MyLocationListener = MyLocationListener()
    private val locationModel = LocationModel()

    var latitude: MutableLiveData<String> = MutableLiveData("")
    var longitude: MutableLiveData<String> = MutableLiveData("")
    var poi: MutableLiveData<String> = MutableLiveData("")
    var weather_tv: MutableLiveData<String> = MutableLiveData("")
    var temp_tv: MutableLiveData<String> = MutableLiveData("")
    var windForce_tv: MutableLiveData<String> = MutableLiveData("")
    var windDirection_tv: MutableLiveData<String> = MutableLiveData("")
    var pm2p5_tv: MutableLiveData<String> = MutableLiveData("")
    var aqi_tv: MutableLiveData<String> = MutableLiveData("")

    var getDoneNum: MutableLiveData<Int> = MutableLiveData(0)
    var allDoneNum: Int = 3


    fun beginToLocate(binding: FragmentLocationBinding) {
        getDoneNum.value = 0
        //BD_map

        //true，表示用户同意隐私合规政策
        //false，表示用户不同意隐私合规政策
        LocationClient.setAgreePrivacy(true)
        //setAgreePrivacy接口需要在LocationClient实例化之前调用
        //如果setAgreePrivacy接口参数设置为了false，则定位功能不会实现
        try {
            // 更新定位客户端
            mLocationClient = LocationClient(MyApplication.context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //QWeather
        //使用 SDK 时，需提前进行账户初始化
        HeConfig.init("HE2211142052181366", "dae5958f9f9c453b8f350c460fa5b321")
        //切换至开发版服务
        HeConfig.switchToDevService()

        //声明LocationClient类
        mLocationClient?.let {
            println("nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn")
            //注册监听函数
            it.registerLocationListener(myListener) // myListener是需要自己定义的class MyLocationListener : BDAbstractLocationListener()
            //设置初始数据
            locationModel.initLocation(it)
            //开始定位
            it.start()
        }
    }


    /***
     * BD_location 回调
     * inner 表示 不是静态内部类
     */
    inner class MyLocationListener : BDAbstractLocationListener() {
        @SuppressLint("SetTextI18n")
        /**
         * 每次start都会调用此方法得到BD定位信息。涉及到修改mutableLiveData，放在ViewModel中。
         */
        override fun onReceiveLocation(location: BDLocation) {
            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            //以下只列举部分获取经纬度相关（常用）的结果信息
            //更多结果信息获取说明，请参照类参考中BDLocation类中的说明
            val latitudeStr = location.latitude //获取纬度信息
            val longitudeStr = location.longitude //获取经度信息
            val radius = location.radius //获取定位精度，默认值为0.0f
            Log.d("纬度", latitudeStr.toString() + "")
            Log.d("经度", longitudeStr.toString() + "")
            latitude.value = if (latitudeStr.toString() == "4.9E-324") "0" else latitudeStr.toString()
            longitude.value = if (longitudeStr.toString() == "4.9E-324") "0" else longitudeStr.toString()
            val coorType = location.coorType
            //获取经纬度坐标类型，以LocationClientOption中设置过的坐标类型为准
            val errorCode = location.locType
            println("error:::::::$errorCode")
            //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明
            val poiList = location.poiList
            //POI信息包括POI ID、名称等，具体信息请参照类参考中POI类的相关说明
            poi.value = poiList?.let { locationModel.getPOIInfo(it) } ?: "null"
            Log.d("POI", poiList?.toString() + "")
            getDoneNum.value = getDoneNum.value?.plus(1)

            println("longitude: ${longitude.value} --- latitude:${latitude.value}")
            @SuppressLint("DefaultLocale") val locationNow =
                String.format("%.2f", longitude.value!!.toFloat()) + "," + String.format("%.2f", latitude.value!!.toFloat()) //保留2位小数

            // 顺便查询天气
            queryWeather(locationNow)
        }
    }

    /**
     * QWeather。涉及到修改MutableLiveData。
     * @param locationNow String
     */
    fun queryWeather(locationNow: String) {
        //天气
        QWeather.getWeatherNow(MyApplication.context, locationNow, Lang.ZH_HANS, Unit.METRIC, object : OnResultWeatherNowListener {
            override fun onError(e: Throwable) {
                Log.i(TAG, "onError: ", e)
                println("Weather Now Error:" + Gson())
            }

            @SuppressLint("SetTextI18n")
            override fun onSuccess(weatherBean: WeatherNowBean) {
                //Log.i(TAG, "getWeather onSuccess: " + new Gson().toJson(weatherBean));
                println("获取天气成功： " + Gson().toJson(weatherBean))
                //先判断返回的status是否正确，当status正确时获取数据，若status不正确，可查看status对应的Code值找到原因
                if (Code.OK == weatherBean.code) {
                    val now = weatherBean.now
                    val tianqi = now.text //当前天气
                    val wendu = now.temp //当前温度 摄氏度
                    val fengli = now.windScale //风力（级）
                    val fengxiang = now.windDir //风向
                    println("hereeeeeeeeeeeeeeeeeeeeeeeeee:$tianqi+$wendu+$fengli+$fengxiang")
                    weather_tv.postValue(tianqi)
                    temp_tv.postValue(wendu)
                    windForce_tv.postValue(fengli)
                    windDirection_tv.postValue(fengxiang)
                    getDoneNum.postValue(getDoneNum.value?.plus(1))
                } else {
                    //在此查看返回数据失败的原因
                    val code = weatherBean.code
                    println("失败代码: $code")
                    //Log.i(TAG, "failed code: " + code);
                }
            }
        })
        //空气
        QWeather.getAirNow(MyApplication.context, locationNow, Lang.ZH_HANS, object : OnResultAirNowListener {
            override fun onError(e: Throwable) {
                Log.i(TAG, "onError: ", e)
                println("Weather Now Error:" + Gson())
            }

            override fun onSuccess(airNowBean: AirNowBean) {
                println("获取空气成功： " + Gson().toJson(airNowBean))
                //先判断返回的status是否正确，当status正确时获取数据，若status不正确，可查看status对应的Code值找到原因
                if (Code.OK == airNowBean.code) {
                    val now = airNowBean.now
                    val pm2_5 = now.pm2p5 //
                    val Aqi = now.aqi //
//                    println("$pm2_5+$Aqi")
                    pm2p5_tv.postValue(pm2_5)
                    aqi_tv.postValue(Aqi)
                    getDoneNum.postValue(getDoneNum.value?.plus(1))
                } else {
                    //在此查看返回数据失败的原因
                    val code = airNowBean.code
                    println("失败代码: $code")
                    //Log.i(TAG, "failed code: " + code);
                }
            }
        })
    }


}