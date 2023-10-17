package com.bjfu.segapp.ui.main

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.baidu.location.*
import com.bjfu.segapp.databinding.FragmentLocationBinding


class LocationFragment : Fragment() {

    companion object {
        fun newInstance() = LocationFragment()
    }


    private lateinit var viewModel: LocationViewModel
    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!


    /**
     * 在此处初始化viewModel。onCreate 方法不能调用 View 类型的对象,完成一些与 UI 无关的 Fragment 初始化
     * @param savedInstanceState Bundle?
     */
    @Deprecated("Deprecated in Java")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[LocationViewModel::class.java]
        // TODO: Use the ViewModel
    }

    /**
     * 初始化与界面相关的内容
     * @param inflater LayoutInflater
     * @param container ViewGroup?
     * @param savedInstanceState Bundle?
     * @return View
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // viewBinding
        _binding = DataBindingUtil.inflate(inflater, com.bjfu.segapp.R.layout.fragment_location, container, false)
        _binding!!.locationVM = viewModel

        binding.startBtn.setOnClickListener {
            viewModel.beginToLocate(binding)
        }
        viewModel.getDoneNum.observe(viewLifecycleOwner, Observer {
            if(it == viewModel.allDoneNum){ // 准备使用dataBinding，但是不能及时更新，必须要刷新fragment后才可以显示
                println("allDone!!!")
                binding.latitude.text = viewModel.latitude.value
                binding.longitude.text = viewModel.longitude.value
                binding.poi.text = viewModel.poi.value
                binding.weatherTv.text = viewModel.weather_tv.value
                binding.tempTv.text = viewModel.temp_tv.value
                binding.windForceTv.text = viewModel.windForce_tv.value
                binding.windDirectionTv.text = viewModel.windDirection_tv.value
                binding.pm2p5Tv.text = viewModel.pm2p5_tv.value
                binding.aqiTv.text = viewModel.aqi_tv.value
            }
        })
        return binding.root
    }


}