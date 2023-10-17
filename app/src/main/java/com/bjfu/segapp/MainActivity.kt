package com.bjfu.segapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bjfu.segapp.ui.main.SectionsPagerAdapter
import com.bjfu.segapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var launcher: ActivityResultLauncher<Intent>

    //  权限请求
    private val mRequestCode = 11111
    private val permissionsStorage = arrayOf(
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.CAMERA"
    )

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)

        verifyStoragePermissions()
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.i("ABCD", "权限申请结果：${it.resultCode == Activity.RESULT_OK}")
            checkAndroid11FilePermission(this) //权限
        }
//        getPermissions() // db定位权限

//        val fab: FloatingActionButton = binding.fab
//
//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }
    }

    /** 检查Android 11或更高版本的文件权限 */
    private fun checkAndroid11FilePermission(activity: FragmentActivity) {
        // Android 11 (Api 30)或更高版本的写文件权限需要特殊申请，需要动态申请管理所有文件的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Log.i("TTZZ", "此手机是Android 11或更高的版本，且已获得访问所有文件权限")
                // TODO requestOtherPermissions() 申请其他的权限
            } else {
                Log.i("TTZZ", "此手机是Android 11或更高的版本，且没有访问所有文件权限")
                showDialog(activity, """本应用需要获取"访问所有文件"权限，请给予此权限，否则无法使用本应用""") {
                    launcher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        } else {
            Log.i("TTZZ", "此手机版本小于Android 11，版本为：API ${Build.VERSION.SDK_INT}，不需要申请文件管理权限")
            // TODO requestOtherPermissions() 申请其他的权限
        }
    }

    private fun showDialog(activity: FragmentActivity, message: String, okClick: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("提示")
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> okClick() }
            .setCancelable(false)
            .show()
    }

    //通过一个函数来申请
    @RequiresApi(Build.VERSION_CODES.R)
    private fun verifyStoragePermissions() {
        try {
            //检测是否有权限
//            val permission = ActivityCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE")
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // 申请权限
                ActivityCompat.requestPermissions(this, permissionsStorage, mRequestCode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

//    private fun getPermissions(): Boolean {
//        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED
//            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED
//        ) { //未开启定位权限
//            //开启定位权限,200是标识码
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 201)
//            false
//        } else {
//            Toast.makeText(this, "已开启定位权限", Toast.LENGTH_LONG).show()
//            true
//        }
//    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            mRequestCode -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[3] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[4] == PackageManager.PERMISSION_GRANTED
                ) { //用户同意权限,执行我们的操作
                } else { //用户拒绝之后,当然我们也可以弹出一个窗口,直接跳转到系统设置页面
                    Toast.makeText(this, "未开启定位权限,请手动到设置去开启权限", Toast.LENGTH_LONG).show()
//                    getPermissions()
                }
            }
            else -> {
                Toast.makeText(this, "错误的响应码！", Toast.LENGTH_LONG).show()
//                getPermissions()
            }
        }
    }
}