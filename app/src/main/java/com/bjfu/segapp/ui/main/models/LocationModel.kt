package com.bjfu.segapp.ui.main.models

import android.util.Log
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.location.Poi
import com.bjfu.segapp.MyApplication
import com.qweather.sdk.bean.air.AirNowBean
import com.qweather.sdk.bean.base.Code
import com.qweather.sdk.bean.base.Lang
import com.qweather.sdk.view.QWeather

class LocationModel {

    /**
     * 转化poiList成字符串
     * @param poiList MutableList<Poi>
     * @return String
     */
    fun getPOIInfo(poiList: MutableList<Poi>): String {
        val stringBuilder = StringBuilder()
        for (p in poiList) {
            stringBuilder.append(p.name).append(" ").append(p.rank).append("\n")
        }
        return stringBuilder.toString()
    }

    /***
     *
     * location配置函数
     */
    fun initLocation(mLocationClient: LocationClient) {
        val option = LocationClientOption()
        option.locationMode = LocationClientOption.LocationMode.Hight_Accuracy
        //每1s刷新一次位置请求
        //可选，设置发起定位请求的间隔，int类型，单位ms
        //如果设置为0，则代表单次定位，即仅定位一次，默认为0
        //如果设置非0，需设置1000ms以上才有效
        option.setScanSpan(0)
        //需要地址具体信息
        option.setIsNeedAddress(true)
        //获取周边信息POI
        option.setIsNeedLocationPoiList(true)
        //可选，设置是否使用gps，默认false
        //使用高精度和仅用设备两种定位模式的，参数必须设置为true
        option.isOpenGps = true
        //可选，设置是否当GPS有效时按照1S/1次频率输出GPS结果，默认false
        option.isLocationNotify = true
        mLocationClient.locOption = option
    }
}