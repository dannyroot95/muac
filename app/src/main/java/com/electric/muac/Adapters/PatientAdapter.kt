package com.electric.muac.Adapters

// PatientAdapter.kt
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.electric.muac.Models.Patient
import com.electric.muac.databinding.PatientItemBinding

class PatientAdapter(
    private var patients: List<Patient>,
    private val onDeleteUser: (Patient) -> Unit,
    private val onViewDetails: (Patient) -> Unit
) : RecyclerView.Adapter<PatientAdapter.UserViewHolder>() {

    class UserViewHolder(val binding: PatientItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = PatientItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = patients[position]
        holder.binding.textViewName.text = "${user.firstName} ${user.lastName}"
        holder.binding.textViewDni.text = user.dni

        // Configurar el click listener para la primera imagen (ver detalles)
        holder.binding.imageViewDetails.setOnClickListener {
            onViewDetails(user) // Llama a la función de callback para mostrar detalles
        }

        // Configurar el click listener para la segunda imagen (eliminar)
        holder.binding.imageViewDelete.setOnClickListener {
            onDeleteUser(user) // Llama a la función de callback para eliminar
        }
    }

    override fun getItemCount() = patients.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateUsers(newPatients: List<Patient>) {
        patients = newPatients
        notifyDataSetChanged()
    }
}
