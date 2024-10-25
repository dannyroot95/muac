package com.electric.muac.Adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.electric.muac.Fragments.PatientFragment
import com.electric.muac.Fragments.RegistersFragment

class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return 2 // Número de tabs
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PatientFragment() // El fragmento que contiene la funcionalidad de usuario
            else -> RegistersFragment() // Otro fragmento
        }
    }
}