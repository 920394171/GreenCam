package com.bjfu.segapp

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * 1. 继承Application实现自己的类
 * 2. 修改AndroidManifest.xml文件，给<application>标签加上android:name=".MyApplication"属性
 */
class MyApplication: Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}