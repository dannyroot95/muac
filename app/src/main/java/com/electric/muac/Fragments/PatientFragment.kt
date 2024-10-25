package com.electric.muac.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.electric.muac.Database.AppDatabase
import com.electric.muac.R
import com.electric.muac.Models.Patient
import com.electric.muac.Adapters.PatientAdapter
import com.electric.muac.databinding.FragmentPatientBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PatientFragment : Fragment() {

    private lateinit var binding: FragmentPatientBinding
    private val database by lazy { AppDatabase.getDatabase(requireContext()) }
    private val firestore by lazy { Firebase.firestore }
    private lateinit var patientAdapter: PatientAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPatientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val filters = arrayOf("DNI", "Nombres", "Historia") // Ajusta según tus datos
        binding.spinnerFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filters)

        binding.search.isIconifiedByDefault = false
        binding.search.requestFocus()

        setupRecyclerView()
        loadUsers()

        binding.btnAddPaciente.setOnClickListener {
            showAddPatientDialog()
        }

        binding.btnMigrate.setOnClickListener {
            showButtonMigrate()
        }

        // Configurar el botón de importación desde Firestore
        binding.btnImport.setOnClickListener {
            importAllPatients()
        }

    }

    private fun saveUser(patient: Patient) {
        CoroutineScope(Dispatchers.IO).launch {
            // Verificar si el DNI ya existe
            val existingUser = database.patientDao().getPatientByDni(patient.dni)
            if (existingUser != null) {
                // Si el usuario ya existe, mostrar un Toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "DNI ${patient.dni} already exists, not saving.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Si el usuario no existe, insertar nuevo usuario
                database.patientDao().insert(patient)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Patient saved successfully.", Toast.LENGTH_SHORT).show()
                    loadUsers() // Cargar nuevamente todos los usuarios y actualizar el RecyclerView
                }
            }
        }
    }



    private fun migrateAndDeleteDataToFirestore() {
        CoroutineScope(Dispatchers.IO).launch {
            val userList = database.patientDao().getAllPatients()
            userList.forEach { user ->
                firestore.collection("patients").document(user.dni)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // DNI exists, handle the conflict or skip
                            CoroutineScope(Dispatchers.IO).launch {
                                database.patientDao().deletePatient(user)  // Eliminar el usuario localmente porque ya existe en Firestore
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), "DNI ${user.dni} already exists in Firestore, deleted locally.", Toast.LENGTH_SHORT).show()
                                    loadUsers()  // Actualizar la lista mostrada en RecyclerView
                                }
                            }
                        } else {
                            // DNI does not exist, safe to add
                            firestore.collection("patients").document(user.dni)
                                .set(user)
                                .addOnSuccessListener {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        database.patientDao().deletePatient(user)  // Eliminar el usuario localmente después de la migración exitosa
                                        withContext(Dispatchers.Main) {
                                            loadUsers()  // Actualizar la lista mostrada en RecyclerView
                                            Toast.makeText(requireContext(), "Patient ${user.dni} added successfully to Firestore and deleted locally.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Error adding user ${user.dni} to Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
            }
        }
    }

    private fun migrateDataToFirestore() {
        CoroutineScope(Dispatchers.IO).launch {
            val userList = database.patientDao().getAllPatients()
            userList.forEach { user ->
                firestore.collection("patients").document(user.dni)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            Toast.makeText(requireContext(), "DNI ${user.dni} already exists in Firestore.", Toast.LENGTH_SHORT).show()
                        } else {
                            // DNI does not exist, safe to add
                            firestore.collection("patients").document(user.dni)
                                .set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "Patient ${user.dni} added successfully to Firestore", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Error adding user ${user.dni} to Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
            }
        }
    }


    private fun showAddPatientDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_user, null)
        val firstNameEditText = dialogView.findViewById<EditText>(R.id.editTextFirstName)
        val lastNameEditText = dialogView.findViewById<EditText>(R.id.editTextLastName)
        val maternalLastNameEditText = dialogView.findViewById<EditText>(R.id.editTextMaternalLastName)
        val dniEditText = dialogView.findViewById<EditText>(R.id.editTextDNI)
        val radioGroupSex = dialogView.findViewById<RadioGroup>(R.id.radioGroupSex)
        val birthdateEditText = dialogView.findViewById<EditText>(R.id.editTextBirthdate)
        val communitySpinner = dialogView.findViewById<Spinner>(R.id.spinnerCommunity)
        val guardianDNIEditText = dialogView.findViewById<EditText>(R.id.editTextGuardianDNI)
        val guardianNameEditText = dialogView.findViewById<EditText>(R.id.editTextGuardianName)
        val historyEditText = dialogView.findViewById<EditText>(R.id.editTextHistory)

        // Configurar el spinner para comunidades
        val communities = arrayOf("Comunidad 1", "Comunidad 2", "Comunidad 3") // Ajusta según tus datos
        communitySpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, communities)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Agregar Paciente")
            .setPositiveButton("Guardar") { dialog, which ->
                val firstName = firstNameEditText.text.toString()
                val lastName = lastNameEditText.text.toString()
                val maternalLastName = maternalLastNameEditText.text.toString()
                val dni = dniEditText.text.toString()
                val sex = if (radioGroupSex.checkedRadioButtonId == R.id.radioButtonMale) "Masculino" else "Femenino"
                val birthdate = birthdateEditText.text.toString()
                val community = communitySpinner.selectedItem.toString()
                val guardianDNI = guardianDNIEditText.text.toString()
                val guardianName = guardianNameEditText.text.toString()
                val history = historyEditText.text.toString()

                // Crear el usuario y guardarlo
                val patient = Patient(dni, firstName, lastName, maternalLastName, sex, birthdate, community, guardianDNI, guardianName,history)
                saveUser(patient)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupRecyclerView() {
        patientAdapter = PatientAdapter(
            patients = emptyList(),
            onDeleteUser = { user -> deleteUser(user) }, // Pasamos la función de eliminar
            onViewDetails = { user -> showUserDetailsDialog(user) } // Pasamos la función para ver detalles
        )
        binding.recyclerView.apply {
            adapter = patientAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadUsers() {
        CoroutineScope(Dispatchers.IO).launch {
            val users = database.patientDao().getAllPatients()
            withContext(Dispatchers.Main) {
                patientAdapter.updateUsers(users)
            }
        }
    }

    private fun deleteUser(patient: Patient) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hey!")
            .setIcon(R.drawable.ic_warning)
            .setCancelable(false)
            .setMessage("Estas seguro de eliminar al paciente ${patient.firstName}${patient.lastName}?")
            .setPositiveButton("Si") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    database.patientDao().deletePatient(patient)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Usuario eliminado", Toast.LENGTH_SHORT).show()
                        loadUsers() // Cargar los usuarios nuevamente para actualizar el RecyclerView
                    }
                }
            }
            .setNegativeButton("No"){ dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun importAllPatients(){
        AlertDialog.Builder(requireContext())
            .setTitle("Alerta!")
            .setIcon(R.drawable.ic_warning)
            .setCancelable(false)
            .setMessage("Al importar, se eliminarán los datos locales")
            .setPositiveButton("Importar") { _, _ ->
                 importDataFromFirestore()
                }.setNegativeButton("Cancelar"){ dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun importDataFromFirestore() {
        // Eliminar los datos locales primero
        CoroutineScope(Dispatchers.IO).launch {
            // Eliminar todos los pacientes de la base de datos local
            database.patientDao().deleteAllPatients()

            // Obtener los pacientes de Firestore
            firestore.collection("patients")
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val importedPatients = mutableListOf<Patient>()

                        // Procesar los documentos de Firestore
                        for (document in result) {
                            val patient = document.toObject(Patient::class.java)
                            importedPatients.add(patient)
                        }

                        // Insertar los pacientes importados en la base de datos local
                        CoroutineScope(Dispatchers.IO).launch {
                            database.patientDao().insertPatients(importedPatients)
                            withContext(Dispatchers.Main) {
                                // Actualizar el RecyclerView después de la importación
                                loadUsers()
                                Toast.makeText(requireContext(), "Datos importados con éxito.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // No hay pacientes en Firestore
                        Toast.makeText(requireContext(), "No se encontraron pacientes en Firestore.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error al importar los datos: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    @SuppressLint("SetTextI18n", "MissingInflatedId")
    private fun showUserDetailsDialog(patient: Patient) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_patient_details, null)

        // Configurar las vistas del diálogo
        val textViewName = dialogView.findViewById<TextView>(R.id.textViewName)
        val textViewDni = dialogView.findViewById<TextView>(R.id.textViewDni)
        val textViewBirthday = dialogView.findViewById<TextView>(R.id.textViewBirthDay)


        textViewName.text = "Nombres : ${patient.firstName} ${patient.lastName}"
        textViewDni.text = "DNI : "+patient.dni
        textViewBirthday.text = "Fecha de nacimiento : "+patient.birthdate

        // Crear y mostrar el diálogo
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setIcon(R.drawable.ic_child)
            .setCancelable(false)
            .setTitle("Detalles del paciente")
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showButtonMigrate(){
        AlertDialog.Builder(requireContext())
            .setTitle("Hey!")
            .setIcon(R.drawable.ic_warning)
            .setMessage("Seleccione una opcion :")
            .setPositiveButton("Migrar y eliminar") { _, _ -> migrateAndDeleteDataToFirestore()
            }
            .setNegativeButton("Solo migrar"){ _, _ -> migrateDataToFirestore()
            }.show()
    }

}