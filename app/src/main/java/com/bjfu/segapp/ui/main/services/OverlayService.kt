package com.bjfu.segapp.ui.main.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import com.bjfu.segapp.R


class OverlayService : Service(), SensorEventListener {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    private lateinit var sensorManager: SensorManager
    private lateinit var orientSensor: Sensor
    private lateinit var xTextView: TextView
    private lateinit var yTextView: TextView
    private lateinit var zTextView: TextView
    private lateinit var notifyTextView: TextView

    private val TAG = "TTZZ"
    private var inRightPosition = ObservableBoolean(false)  // 标记当前是否在正确的位置上
    private var ableToShift = ObservableBoolean(false) // 标记是否可以进行移动
    private var timerHandler = Handler() // Handler对象，用于发送计时器的任务
    private var timerRunnable: Runnable? = null // Runnable对象，用于执行计时任务
    private var timerStartTime = 0L // 记录计时开始时间
    private val DELAY_MS = 3000L // 计时器触发时间，单位毫秒

    private val notifyText1 = "请调整相机位置至水平"
    private val notifyText2 = "很好，请继续保持"
    private val wrongColorTV = "#F4511E"
    private val rightColorTV = "#33E23B"


    @SuppressLint("InflateParams", "SetTextI18n")
    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            orientSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)


        // 获取 WindowManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 创建悬浮窗视图
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_view, null)

        // 设置旋转中心
        overlayView.pivotX = 100f
        overlayView.pivotY = 100f
        xTextView = overlayView.findViewById(R.id.x_tv)
        yTextView = overlayView.findViewById(R.id.y_tv)
        zTextView = overlayView.findViewById(R.id.z_tv)
        notifyTextView = overlayView.findViewById(R.id.notifyTextView)

        notifyTextView.text = notifyText1

        // 启动传感器监听器
        start()

        // 设置 LayoutParams，把视图放到屏幕的右下角
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
//        params.gravity =  Gravity.TOP or Gravity.START
        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_USER


        // 添加回调
        inRightPosition.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if (inRightPosition.get()) {
                    // 如果位置正确，则开始计时
                    startTimer()

                    xTextView.setTextColor(Color.GREEN)
                    yTextView.setTextColor(Color.GREEN)
                    zTextView.setTextColor(Color.GREEN)
                } else {
                    // 如果位置错误，则取消当前计时任务
                    timerRunnable?.let { timerHandler.removeCallbacks(it) }
                    ableToShift.set(false)
                    notifyTextView.text = notifyText1
                    notifyTextView.setTextColor(Color.parseColor(wrongColorTV))

                    xTextView.setTextColor(Color.WHITE)
                    yTextView.setTextColor(Color.WHITE)
                    zTextView.setTextColor(Color.WHITE)
                }
            }
        })

        // 添加回调
        ableToShift.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if (ableToShift.get()) {
                    xTextView.setTextColor(Color.GREEN)
                    yTextView.setTextColor(Color.GREEN)
                    zTextView.setTextColor(Color.GREEN)
                } else {
                    xTextView.setTextColor(Color.WHITE)
                    yTextView.setTextColor(Color.WHITE)
                    zTextView.setTextColor(Color.WHITE)
                }
            }
        })


        // 添加悬浮窗视图到 WindowManager
        windowManager.addView(overlayView, params)
    }

    @SuppressLint("SetTextI18n")
    fun startTimer() {
        // 重置计时器状态
        ableToShift.set(false)

        // 重新开始计时
        timerStartTime = System.currentTimeMillis()

        // 如果计时器已经在运行，则取消当前任务
        timerRunnable?.let { timerHandler.removeCallbacks(it) }

        // 创建一个新的计时器任务
        timerRunnable = Runnable {
            // 计算时间差，如果超过3秒，设置标记为true
            notifyTextView.setTextColor(Color.parseColor(rightColorTV))
            if (System.currentTimeMillis() - timerStartTime >= DELAY_MS) {
                ableToShift.set(true)
                timerRunnable = null // 清空Runnable对象
            } else {
                // 如果时间未超过3秒，继续执行计时任务
                notifyTextView.text = notifyText2 + "${((DELAY_MS - System.currentTimeMillis() + timerStartTime) * 0.001).toInt()}秒"
                timerHandler.postDelayed(timerRunnable!!, 100L)
            }
        }

        // 开始执行计时器任务
        timerHandler.post(timerRunnable!!)
    }


    override fun onDestroy() {
        super.onDestroy()

        // 关闭传感器监听
        stop()

        // Remove overlay view from window manager
        windowManager.removeView(overlayView)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.e(TAG, "onTaskRemoved: service停止")
        stopSelf()
    }


    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent) {
        event.let {
            if (it.sensor.type == Sensor.TYPE_ORIENTATION) {
//                Log.e(TAG, "onSensorChanged: is Orientation Changed!")
                xTextView.text = "Z-航向角: " + event.values[0].toString()
                yTextView.text = "X-俯仰角: " + event.values[1].toString()
                zTextView.text = "Y-翻滚角: " + event.values[2].toString()
//                overlayView.rotation = event.values[2] // 让overlayView随着翻滚角而

                if ((event.values[1] in -170.0..-160.0 || event.values[1] in -10.0..10.0 || event.values[1] in 170.0..180.0)
                    && event.values[2] in 80.0..90.0
                )
                    inRightPosition.set(true)
                else
                    inRightPosition.set(false)
            }
        }
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // do nothing
    }

    private fun start() {
        sensorManager.registerListener(this, orientSensor, SensorManager.SENSOR_DELAY_NORMAL) // about 200 ms
    }

    private fun stop() {
        sensorManager.unregisterListener(this)
    }
}
