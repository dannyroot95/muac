package com.electric.muac.Adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.electric.muac.Activitys.RegistersDetailsActivity
import com.electric.muac.Models.Registers
import com.electric.muac.databinding.RegisterItemBinding

class RegistersAdapter(
    private val context: Context,
    private val registers: List<Registers>,
    private val onDeleteClick: (Registers) -> Unit
) : RecyclerView.Adapter<RegistersAdapter.RegisterViewHolder>() {

    inner class RegisterViewHolder(val binding: RegisterItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegisterViewHolder {
        val binding = RegisterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RegisterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RegisterViewHolder, position: Int) {
        val register = registers[position]
        with(holder.binding) {
            var status = "Migrado"
            textViewImc.text = "IMC: ${register.imc}"
            textViewMuac.text = "MUAC: ${register.muac}"
            textViewName.text = register.fullname
            textViewDni.text = "DNI: ${register.dni}"

            if(register.mode != "cloud"){
                status = "En local"
            }else{
                imageViewDelete.visibility = View.GONE
            }

            textStatus.text = "Estado: ${status}"

            // Configurar el botón de eliminar
            imageViewDelete.setOnClickListener {
                onDeleteClick(register)
            }

            // Configurar el botón de detalles
            imageViewDetails.setOnClickListener {
                val intent = Intent(context, RegistersDetailsActivity::class.java)
                intent.putExtra("register", register) // Pasar el objeto registro
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = registers.size
}
