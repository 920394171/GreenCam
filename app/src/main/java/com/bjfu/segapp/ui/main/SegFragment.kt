package com.bjfu.segapp.ui.main

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bjfu.segapp.MyApplication
import com.bjfu.segapp.databinding.FragmentSegBinding
import com.bjfu.segapp.ui.main.utils.ScannerUtil
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class SegFragment : Fragment() {

    companion object {
        fun newInstance() = SegFragment()
    }

    private lateinit var viewModel: SegViewModel
    private var _binding: FragmentSegBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    var imageUri: Uri? = null
    var path: String? = null
    var picpath: String? = null

    val TAKE_PHOTO = 1 //声明一个请求码，用于识别返回的结果
    val SCAN_OPEN_PHONE = 2 // 相册
    val GET_PICTURE_REQUEST_CODE = 111
    private val REQUEST_READ_STORAGE = 101

    /**
     * onActivityCreated()晚于onCreate()方法，改成onCreate，并在此处初始化viewModel
     * @param savedInstanceState Bundle?
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SegViewModel::class.java]
        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSegBinding.inflate(inflater, container, false)

//        binding.imageView.setImageBitmap(getIntent("CameraxActivity").getParcelableExtra("bitmap"))

        binding.btnTakePhoto.setOnClickListener {
            choosePic()
        }

        binding.restartButton.setOnClickListener {
            binding.imageView.setImageDrawable(null)
        }

        viewModel.segHasDone.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it) {
                activity?.runOnUiThread(Runnable {
                    viewModel.mImageView!!.setImageBitmap(viewModel.transferredBitmap)
                    viewModel.mButtonSegment!!.isEnabled = true
                    viewModel.mButtonSegment!!.text = "segment"
                    viewModel.mProgressBar!!.visibility = ProgressBar.INVISIBLE
                    viewModel.mRatios!!.text = generateOutputString(viewModel.ratio)
                })
            }
        })

        viewModel.beginToSeg(binding)

        return binding.root
    }

    fun generateOutputString(ratio: Array<Double>): String {
        val classesList = arrayOf("background", "water", "architecture", "facility", "sky",
            "flat", "plant_tree", "plant_shrub", "plant_lawn", "plant_flower")

        // 验证ratio的长度是否与classes_list匹配
        if (ratio.size != classesList.size) {
            throw IllegalArgumentException("Ratio array and classes list do not have the same size.")
        }

        val stringBuilder = StringBuilder()
        for (i in ratio.indices step 2) {
            val percentage1 = String.format("%.2f", ratio[i] * 100)
            stringBuilder.append("${classesList[i]}: $percentage1%")

            if (i + 1 < ratio.size) {
                val percentage2 = String.format("%.2f", ratio[i + 1] * 100)
                stringBuilder.append("\t\t${classesList[i + 1]}: $percentage2%")
            }

            stringBuilder.append("\n")
        }

        return stringBuilder.toString().trim() // 返回结果字符串
    }

    /**
     * 转接相机或相册
     */
    private fun choosePic() {
//        注释。下列代码用以在相册选择或者拍摄图片
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            .setTitle("请选择图片") //设置对话框 标题
            .setItems(viewModel.items, DialogInterface.OnClickListener { dialog, which ->
                if (which == 0) {
                    openCamera()
                } else {
                    openGallery()
                }
            })
        builder.create().show()

//        requestOverlayPermission()
//
//        startActivityForResult(Intent(activity, CameraxActivity::class.java), GET_PICTURE_REQUEST_CODE)
    }

    /**
     * 请求overlay权限
     */
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context?.packageName}")
            )
            startActivity(intent)
        }
    }

    /**
     * 打开相册
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, SCAN_OPEN_PHONE)
    }

    /**
     * 打开相机
     */
    private fun openCamera() {
        val imageName = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        //        File outputImage=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/data/com.example.woundapplication/"+imageName+".jpg");
        val outputImage: File = File(requireContext().externalCacheDir, "$imageName.jpg")
        Objects.requireNonNull(outputImage.parentFile).mkdirs()
        //        Log.e("", outputImage.getAbsolutePath());
        /*
            创建一个File文件对象，用于存放摄像头拍下的图片，
            把它存放在应用关联缓存目录下，调用getExternalCacheDir()可以得到这个目录，为什么要
            用关联缓存目录呢？由于android6.0开始，读写sd卡列为了危险权限，使用的时候必须要有权限，
            应用关联目录则可以跳过这一步
         */
        try  //判断图片是否存在，存在则删除在创建，不存在则直接创建
        {
            if (outputImage.exists()) {
                outputImage.delete()
            }
            val a = outputImage.createNewFile()
            Log.e("createNewFile", a.toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (Build.VERSION.SDK_INT >= 24) //判断安卓的版本是否高于7.0，高于则调用高于的方法，低于则调用低于的方法
        //把文件转换成Uri对象
        /*
                    因为android7.0以后直接使用本地真实路径是不安全的，会抛出异常。
                    FileProvider是一种特殊的内容提供器，可以对数据进行保护
                     */ {
            imageUri = FileProvider.getUriForFile(
                MyApplication.context,
                "com.ttzz.segapp.fileprovider", outputImage
            )
            //对应Mainfest中的provider
//            imageUri=Uri.fromFile(outputImage);
            path = imageUri!!.path
            Log.e(">7:", path!!)
        } else {
            imageUri = Uri.fromFile(outputImage)
            path = imageUri!!.path
            Log.e("<7:", imageUri!!.path!!)
        }

        //使用隐示的Intent，系统会找到与它对应的活动，即调用摄像头，并把它存储
        val intent0 = Intent("android.media.action.IMAGE_CAPTURE")
//        intent0.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent0, TAKE_PHOTO)
    }

    /**
     * @param requestCode 请求代码
     * @param resultCode  返回值代码
     * @param data        携带信息
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val imgResult = binding.imageView
        when (requestCode) {
            TAKE_PHOTO -> if (resultCode == Activity.RESULT_OK) {
                // 说明成功完成拍摄
//                picpath = imageUri!!.path.toString()
//                Log.e("", imageUri!!.authority!!)
//                Log.e("picpath", picpath!!)
//                viewModel.resolveRotate(picpath!!)
//                viewModel.mBitmap = BitmapFactory.decodeStream(imageUri?.let { MyApplication.context.contentResolver.openInputStream(it) })

                // 得到拍摄的位图结果
                val bundle: Bundle? = data?.extras
                viewModel.mBitmap = bundle!!["data"] as Bitmap
                // 保存到系统相册
                ScannerUtil.saveImageToGallery(MyApplication.context, viewModel.mBitmap!!)

                viewModel.mBitmap?.let {
                    // 复制一份用以提交到服务器
                    viewModel.originBitMap.value = Bitmap.createScaledBitmap(
                        it,
                        it.width,
                        it.height,
                        true
                    )
                }
                imgResult.setImageBitmap(viewModel.mBitmap)
                imgResult.invalidate()
            }

            SCAN_OPEN_PHONE -> if (resultCode == Activity.RESULT_OK) {
//                // 说明成功完成相册图片选择
//
//                // 读取选择的图片
//                val selectImage = data!!.data
//                val FilePathColumn = arrayOf(MediaStore.Images.Media.DATA)
//                val cursor: Cursor? = selectImage?.let {
//                    MyApplication.context.contentResolver.query(
//                        it,
//                        FilePathColumn, null, null, null
//                    )
//                }
//                cursor?.moveToFirst()
//                //从数据视图中获取已选择图片的路径
//                val columnIndex = cursor?.getColumnIndex(FilePathColumn[0])
//                picpath = columnIndex?.let { cursor.getString(it) }
//                picpath?.let { Log.e("picpath", it) }
//                cursor?.close()
//                // 文件解码成位图
//                viewModel.mBitmap = BitmapFactory.decodeFile(picpath)
//                picpath?.let { viewModel.resolveRotate(it) }
//                imgResult.setImageBitmap(viewModel.mBitmap)
////                viewModel.mBitmap = BitmapFactory.decodeStream(imageUri?.let { MyApplication.context.contentResolver.openInputStream(it) })
//                viewModel.mBitmap?.let {
//                    viewModel.originBitMap.value = Bitmap.createScaledBitmap(
//                        it,
//                        it.width,
//                        it.height,
//                        true
//                    )
//                }
//                imgResult.invalidate()
//            }
//            GET_PICTURE_REQUEST_CODE -> if (resultCode == CameraxActivity.GET_PICTURE_RESPONSE_CODE) {
//                val bitmap = data?.getParcelableExtra("bitmap", Bitmap::class.java)
//                binding.imageView.setImageBitmap(bitmap)
//            }


                // 说明成功完成相册图片选择

                // 读取选择的图片
                val selectImage = data!!.data
                // 使用ContentResolver来处理图片
                try {
                    viewModel.mBitmap = BitmapFactory.decodeStream(
                        selectImage?.let { MyApplication.context.contentResolver.openInputStream(it) }
                    )
                    viewModel.mBitmap?.let {
                        viewModel.originBitMap.value = Bitmap.createScaledBitmap(
                            it,
                            it.width,
                            it.height,
                            true
                        )
                    }
                    imgResult.setImageBitmap(viewModel.mBitmap)
                    imgResult.invalidate()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Handle the error appropriately
                }
            }

            GET_PICTURE_REQUEST_CODE -> if (resultCode == CameraxActivity.GET_PICTURE_RESPONSE_CODE) {
                val bitmap = data?.getParcelableExtra("bitmap", Bitmap::class.java)
                binding.imageView.setImageBitmap(bitmap)
            }

            else -> {}
        }
    }


}