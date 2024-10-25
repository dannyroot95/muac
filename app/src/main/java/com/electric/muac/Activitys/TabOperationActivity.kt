package com.electric.muac.Activitys

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.electric.muac.R
import com.electric.muac.Adapters.ViewPagerAdapter
import com.electric.muac.databinding.ActivityTabOperationBinding
import com.google.android.material.tabs.TabLayoutMediator

class TabOperationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTabOperationBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTabOperationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar ViewPager2 con Fragmentos
        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        // Conectar el TabLayout con el ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            // Inflar el layout personalizado para cada pestaña
            val tabView = LayoutInflater.from(this).inflate(R.layout.tab_item, null)

            val tabIcon = tabView.findViewById<ImageView>(R.id.tabIcon)
            val tabText = tabView.findViewById<TextView>(R.id.tabText)

            // Asignar el texto y el icono según la posición
            when (position) {
                0 -> {
                    tabText.text = "Pacientes"
                    tabIcon.setImageResource(R.drawable.ic_child) // Asegúrate de tener este ícono en tu carpeta drawable
                }
                1 -> {
                    tabText.text = "Mis registros"
                    tabIcon.setImageResource(R.drawable.ic_registers) // Asegúrate de tener este ícono en tu carpeta drawable
                }
            }

            // Asignar la vista personalizada al tab
            tab.customView = tabView
        }.attach()

        binding.fab.setOnClickListener {
            Toast.makeText(this,"Internet Activo",Toast.LENGTH_SHORT).show()
        }

    }
}