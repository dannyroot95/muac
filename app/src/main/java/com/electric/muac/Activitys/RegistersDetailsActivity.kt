package com.electric.muac.Activitys

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.electric.muac.Models.Registers
import com.electric.muac.databinding.ActivityRegistersDetailsBinding

class RegistersDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistersDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistersDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener el objeto Registers del Intent
        val register = intent.getParcelableExtra<Registers>("register")

        register?.let {
            // Mostrar los detalles del registro en esta Activity
            binding.dni.text = it.dni
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

}