package com.bjfu.segapp.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.bjfu.segapp.MyApplication
import com.bjfu.segapp.databinding.FragmentClientBinding

class ClientFragment : Fragment() {

    companion object {
        fun newInstance() = ClientFragment()
    }

    private lateinit var viewModel: ClientViewModel
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var segViewModel: SegViewModel
    private var _binding: FragmentClientBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    /**
     * onActivityCreated()晚于onCreate()方法，改成onCreate，并在此处初始化viewModel
     * @param savedInstanceState Bundle?
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[ClientViewModel::class.java]


        // TODO: Use the ViewModel

    }

    /**
     * 只需要返回root即可，_binding就是viewBinding，但操作的时候调用binding即可，binding只有可读权限，更加安全。<br/>
     * onCreateView是每次现形时都会调用，onCreate就是一开始调用。
     * @param inflater LayoutInflater
     * @param container ViewGroup?
     * @param savedInstanceState Bundle?
     * @return View? 返回root即可
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientBinding.inflate(inflater, container, false)
        val root = binding.root

        /* fragment里面使用viewLifecycleOwner代替this */
        viewModel.imgHasPosted.observe(viewLifecycleOwner, Observer { if (it) viewModel.beginToast(viewModel.imgPath.value!!) })
        binding.postBeforeBtn.setOnClickListener { viewModel.postImgAndLocationInfo(it, binding, requireActivity()) }

        return root
    }

}