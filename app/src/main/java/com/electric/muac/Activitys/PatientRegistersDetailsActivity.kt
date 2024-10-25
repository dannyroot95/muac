package com.electric.muac.Activitys

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.electric.muac.databinding.ActivityPatientRegistersDetailsBinding
import android.app.AlertDialog
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.electric.muac.Activitys.Utils.RulerView
import com.electric.muac.Database.AppDatabase
import com.electric.muac.Models.Patient
import com.electric.muac.Models.Registers
import com.electric.muac.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*

class PatientRegistersDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientRegistersDetailsBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var rulerView: RulerView
    private lateinit var seekBar: SeekBar
    private lateinit var muac: String
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private var latitude : Double = 0.0
    private var longitude : Double = 0.0
    private var fullname : String = ""
    private var dni : String = ""
    private var history : String = ""
    private var age : String = ""

    private val database by lazy { AppDatabase.getDatabase(this) }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientRegistersDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        // Habilita el botón de retroceso en el ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        muac = "0.0"
        rulerView = binding.rulerView
        seekBar = binding.seekBar
        seekBar.max = 260
        seekBar.progress = 0
        rulerView.setMeasurement(0.0f)

        // Obtener el objeto Patient del Intent
        val patient = intent.getParcelableExtra<Patient>("selected_patient")
        patient?.let {
            // Mostrar los detalles del paciente en esta Activity
            fullname = "${it.firstName} ${it.lastName}"
            dni = it.dni
            history = it.history
            age = calculateAge(it.birthdate).toString()
            history = it.history

            binding.toolbarTitle.text = fullname
            binding.textPatientDni.setText(dni)
            binding.edtAge.setText(age)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val measurement = progress / 10.0f
                rulerView.setMeasurement(measurement)
                muac = measurement.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Implementar el listener del botón de regresar
        binding.back.setOnClickListener { returnTab() }

        binding.saveButton.setOnClickListener {
            saveRegister()
        }

        // Inicializa el LocationManager y LocationListener para obtener la ubicación
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Aquí manejas la ubicación obtenida
                 latitude = location.latitude
                 longitude = location.longitude
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // Verificar si los permisos ya han sido concedidos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permisos
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Si los permisos ya están concedidos, verificar y solicitar actualizaciones
            checkGpsEnabledAndRequestUpdates()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // El permiso ha sido concedido, solicita las actualizaciones de ubicación
                checkGpsEnabledAndRequestUpdates()
            } else {
                // Permiso denegado, mostrar un mensaje o manejar la denegación del permiso
                AlertDialog.Builder(this)
                    .setTitle("Permiso de ubicación denegado")
                    .setMessage("La aplicación necesita acceder a tu ubicación para funcionar correctamente.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun checkGpsEnabledAndRequestUpdates() {
        // Verificar si el GPS está habilitado
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Si está habilitado, solicitar actualizaciones de ubicación
            requestLocationUpdates()
        } else {
            // Si no está habilitado, mostrar el diálogo y seguir verificando
            showEnableGpsDialog()
        }
    }

    private fun showEnableGpsDialog() {
        AlertDialog.Builder(this)
            .setMessage("Por favor, habilita el GPS para obtener tu ubicación.")
            .setPositiveButton("Configuración") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                checkGpsStatusPeriodically()  // Sigue verificando si el GPS está habilitado
            }
            .setNegativeButton("Cancelar") { _, _ ->
                showEnableGpsDialog()  // Si el usuario cancela, sigue mostrando el diálogo
            }
            .setCancelable(false)
            .show()
    }

    private fun checkGpsStatusPeriodically() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    // Si el GPS aún no está activado, muestra el diálogo nuevamente
                    showEnableGpsDialog()
                } else {
                    // Si el GPS está activado, solicita actualizaciones de ubicación
                    requestLocationUpdates()
                }
            }
        }, 2000)  // Verifica cada 2 segundos
    }

    private fun requestLocationUpdates() {
        // Solicitar actualizaciones de ubicación utilizando GPS
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
    }

    override fun onBackPressed() {
        returnTab()
    }

    private fun returnTab() {
        AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_warning)
            .setCancelable(false)
            .setTitle("Alerta!")
            .setMessage("¿Está seguro de salir?")
            .setPositiveButton("Sí") { _, _ ->
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun calculateAge(birthdate: String): Int {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val birthday = LocalDate.parse(birthdate, formatter)
        val today = LocalDate.now()
        return Period.between(birthday, today).years
    }

    private fun saveRegister() {
        val weightText = binding.edtWeight.text.toString()
        val sizeText = binding.edtSize.text.toString()

        // Verificar si el peso y la talla tienen datos válidos
        val weight = if (weightText.isNotEmpty()) weightText.toIntOrNull() else 0
        val size = if (sizeText.isNotEmpty()) sizeText.toDoubleOrNull() else 0.0

        var imc = 0f // Valor inicial para el IMC
        if (weight != null && size != null && size > 0.0) {
            // Calcular el IMC si el peso y la talla son válidos
            val sizeInMeters = size / 100
            imc = (weight / (sizeInMeters * sizeInMeters)).toFloat()
        }

        val muacValue = muac.toFloat()
        val imcStatus = getImcStatus(imc)

        if (muacValue != 0f) {
            val mode = "localy"
            val dateRegister = System.currentTimeMillis()
            val id = dni + dateRegister.toString()
            val register = Registers(id, fullname, dni, age, history, imc, muacValue, latitude, longitude, weight!!, size!!, mode,
                dateRegister, auth.currentUser!!.uid,imcStatus)

            // Verificar si ya existe un registro con el mismo DNI antes de guardar
            CoroutineScope(Dispatchers.IO).launch {
                val lastRegister = database.registersDao().getLastRegisterByDni(dni)

                val shouldRegister: Boolean = if (lastRegister != null) {
                    // Convertir el timestamp del último registro y el nuevo registro a meses y años
                    val lastCalendar = Calendar.getInstance().apply { timeInMillis = lastRegister.timestamp }
                    val newCalendar = Calendar.getInstance().apply { timeInMillis = dateRegister }

                    val lastMonth = lastCalendar.get(Calendar.MONTH)
                    val lastYear = lastCalendar.get(Calendar.YEAR)
                    val newMonth = newCalendar.get(Calendar.MONTH)
                    val newYear = newCalendar.get(Calendar.YEAR)

                    // Comparar el mes y el año del último registro con el nuevo registro
                    if (newYear > lastYear || (newYear == lastYear && newMonth > lastMonth)) {
                        true // El nuevo registro es más reciente, se permite registrar
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@PatientRegistersDetailsActivity, "Ya existe un registro para este mes.", Toast.LENGTH_SHORT).show()
                        }
                        false // No se permite registrar
                    }
                } else {
                    // No existe un registro anterior, se permite registrar
                    true
                }

                // Si la verificación es exitosa, mostrar el diálogo de confirmación y registrar
                if (shouldRegister) {
                    runOnUiThread {
                        AlertDialog.Builder(this@PatientRegistersDetailsActivity)
                            .setIcon(R.drawable.ic_warning)
                            .setCancelable(false)
                            .setTitle("Hey!")
                            .setMessage("¿Está seguro de almacenar estos datos?")
                            .setPositiveButton("Sí") { _, _ ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.registersDao().insert(register)
                                    runOnUiThread {
                                        Toast.makeText(this@PatientRegistersDetailsActivity, "Registro almacenado!", Toast.LENGTH_SHORT).show()
                                        finish()
                                    }
                                }
                            }
                            .setNegativeButton("No", null)
                            .show()
                    }
                }
            }

        } else {
            Toast.makeText(this, "Ingrese el valor de la cinta MUAC", Toast.LENGTH_SHORT).show()
        }
    }

    fun getImcStatus(imc: Float): String {
        return when {
            imc == 0f -> "Sin registro"
            imc < 18.5 -> "Bajo peso"
            imc in 18.5..24.9 -> "Peso normal"
            imc in 25.0..29.9 -> "Sobrepeso"
            imc in 30.0..34.9 -> "Obesidad grado I"
            imc in 35.0..39.9 -> "Obesidad grado II"
            imc >= 40.0 -> "Obesidad grado III"
            else -> "IMC inválido"
        }
    }

}