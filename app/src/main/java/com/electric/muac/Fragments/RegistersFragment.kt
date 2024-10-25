package com.electric.muac.Fragments

import android.annotation.SuppressLint
import android.content.Intent
import com.electric.muac.R
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.electric.muac.Activitys.PatientRegistersDetailsActivity
import com.electric.muac.Adapters.RegistersAdapter
import com.electric.muac.Database.AppDatabase
import com.electric.muac.Models.Patient
import com.electric.muac.Models.Registers
import com.electric.muac.databinding.FragmentRegistersBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistersFragment : Fragment() {

    private var _binding: FragmentRegistersBinding? = null
    private val database by lazy { AppDatabase.getDatabase(requireContext()) }
    private lateinit var adapter: RegistersAdapter
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }  // Firestore


    // Esta propiedad solo es válida entre onCreateView y onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegistersBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingInflatedId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.search.isIconifiedByDefault = false
        binding.search.requestFocus()

        val filters = arrayOf("DNI", "Nombres", "Historia") // Ajusta según tus datos
        binding.spinnerFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filters)
        // Configurar el RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            val registers = database.registersDao().getAllRegisters()
            // Pasar el contexto al Adapter
            adapter = RegistersAdapter(requireContext(), registers) { register ->
                // Acción para eliminar el registro
                deleteRegister(register)
            }

            binding.recyclerView.adapter = adapter
        }
        // Configurar el botón para agregar un nuevo registro
        binding.btnAddRegister.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.selected_patient, null)

            lifecycleScope.launch {
                try {
                    val spinnerSelectedPatient = dialogView.findViewById<Spinner>(R.id.spinnerSelectPatient)
                    val patients = database.patientDao().getAllPatients()
                    val patientNames = patients.map { "${it.firstName} ${it.lastName}" }

                    if (patients.isNotEmpty()) {
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, patientNames)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerSelectedPatient.adapter = adapter
                    } else {
                        Toast.makeText(requireContext(), "No se encontraron pacientes!", Toast.LENGTH_SHORT).show()
                    }

                    var selectedPatient: Patient? = null  // Variable para almacenar el paciente seleccionado

                    spinnerSelectedPatient.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            selectedPatient = patients[position]  // Asigna el paciente seleccionado
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }

                    // Crear y mostrar el diálogo
                    AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .setIcon(R.drawable.ic_child)
                        .setCancelable(false)
                        .setTitle("Seleccione un paciente")
                        .setPositiveButton("Registrar") { _, _ ->
                            // Al presionar "Registrar", enviar el paciente a la nueva Activity
                            selectedPatient?.let { patient ->
                                val intent = Intent(requireContext(), PatientRegistersDetailsActivity::class.java)
                                intent.putExtra("selected_patient", patient)  // Pasar el objeto Parcelable
                                startActivity(intent)
                            } ?: Toast.makeText(requireContext(), "No se ha seleccionado un paciente", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cerrar", null)
                        .show()

                } catch (e: Exception) {
                    Log.e("RegistersFragment", "Error: ${e.message}", e)
                    Toast.makeText(requireContext(), "An error occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnMigrate.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setIcon(R.drawable.ic_warning)
                .setTitle("Alerta!")
                .setCancelable(false)
                .setMessage("¿Está seguro de migrar los datos locales?")
                .setPositiveButton("Sí") { _, _ ->
                    migrateLocalRegistersToCloud()
                } .setNegativeButton("No", null)
                .show()

        }

    }

    private fun deleteRegister(register: Registers) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmación")
            .setMessage("¿Está seguro de que desea eliminar este registro?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    database.registersDao().deleteRegister(register)
                    // Actualizar la lista después de eliminar
                    val updatedRegisters = database.registersDao().getAllRegisters()
                    adapter = RegistersAdapter(requireContext(), updatedRegisters) { reg -> deleteRegister(reg) }
                    binding.recyclerView.adapter = adapter
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun migrateLocalRegistersToCloud() {
        lifecycleScope.launch(Dispatchers.IO) {
            val localRegisters = database.registersDao().getLocalRegisters()  // Obtener solo los registros con mode "localy"

            localRegisters.forEach { register ->
                // Verificar si el registro ya existe en Firestore
                firestore.collection("registers").document(register.id)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            Toast.makeText(requireContext(),"Registro con ID ${register.id} ya existe.",Toast.LENGTH_SHORT).show()
                        } else {
                            // Migrar el registro a Firestore
                            register.mode = "cloud"
                            firestore.collection("registers").document(register.id)
                                .set(register)
                                .addOnSuccessListener {
                                    Log.d("Migration", "Registro ${register.id} migrado exitosamente.")
                                    // Actualizar el modo a "cloud"
                                    register.mode = "cloud"
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        database.registersDao().update(register)  // Actualizar el registro en la base de datos local

                                        // Cuando se actualice el último registro, actualiza el RecyclerView
                                        if (register == localRegisters.last()) {
                                            withContext(Dispatchers.Main) {
                                                updateRecyclerView()  // Llamar a la función para actualizar el RecyclerView
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Migration", "Error al migrar el registro ${register.id}: ${e.message}")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Migration", "Error al verificar si el registro ${register.id} existe en Firestore: ${e.message}")
                    }
            }
        }
    }

    private fun updateRecyclerView() {
        // Volver a cargar los registros desde la base de datos y actualizar el RecyclerView
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedRegisters = database.registersDao().getAllRegisters()  // Obtener todos los registros actualizados
            withContext(Dispatchers.Main) {
                adapter = RegistersAdapter(requireContext(), updatedRegisters) { register ->
                    deleteRegister(register)
                }
                binding.recyclerView.adapter = adapter
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedRegisters = database.registersDao().getAllRegisters()  // Obtener todos los registros actualizados
            withContext(Dispatchers.Main) {
                adapter = RegistersAdapter(requireContext(), updatedRegisters) { register ->
                    deleteRegister(register)
                }
                binding.recyclerView.adapter = adapter
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedRegisters = database.registersDao().getAllRegisters()  // Obtener todos los registros actualizados
            withContext(Dispatchers.Main) {
                adapter = RegistersAdapter(requireContext(), updatedRegisters) { register ->
                    deleteRegister(register)
                }
                binding.recyclerView.adapter = adapter
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedRegisters = database.registersDao().getAllRegisters()  // Obtener todos los registros actualizados
            withContext(Dispatchers.Main) {
                adapter = RegistersAdapter(requireContext(), updatedRegisters) { register ->
                    deleteRegister(register)
                }
                binding.recyclerView.adapter = adapter
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedRegisters = database.registersDao().getAllRegisters()  // Obtener todos los registros actualizados
            withContext(Dispatchers.Main) {
                adapter = RegistersAdapter(requireContext(), updatedRegisters) { register ->
                    deleteRegister(register)
                }
                binding.recyclerView.adapter = adapter
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

