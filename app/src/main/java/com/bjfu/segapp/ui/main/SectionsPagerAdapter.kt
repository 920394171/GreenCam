package com.bjfu.segapp.ui.main

import android.content.Context
import android.util.Log
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.bjfu.segapp.R

private val TAB_TITLES = arrayOf(
        R.string.tab_text_1,
        R.string.tab_text_2,
        R.string.tab_text_3
)

const val TAG:String = "TTZZ"

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager)
    : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return when(position){
            0 -> {
                Log.e(TAG, "getItem: 0")
                LocationFragment.newInstance()
            }
            1 -> {
                Log.e(TAG, "getItem: 1")
//                PlaceholderFragment.newInstance(position + 1)
                SegFragment.newInstance()
            }
            else->{
                Log.e(TAG, "getItem: 2")
                ClientFragment.newInstance()
            }
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        // Show 2 total pages.
        return 3
    }
}