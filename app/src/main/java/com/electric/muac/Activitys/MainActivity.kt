package com.electric.muac.Activitys

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.electric.muac.R
import com.electric.muac.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        // Configuración de Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id)) // Tu web client ID de Firebase
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            if(email != "" && password != ""){
                signInWithEmail(email, password)
            }else{
                Toast.makeText(this, "Complete los campos", Toast.LENGTH_SHORT).show()
            }

        }

        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.txtGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }


    private fun signInWithEmail(email: String, password: String) {
        setLoadingState(true)
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Inicio de sesión exitoso, consulta a Firestore
                        val firestore = FirebaseFirestore.getInstance()
                        firestore.collection("users").document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                setLoadingState(false)
                                if (document.exists() && document.data != null) {
                                    val typeUser = document.getString("typeUser")
                                    saveUserDetailsInPreferences(user.uid,typeUser)
                                    val intent = Intent(this, TabOperationActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "Usuario no encontrado!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                setLoadingState(false)
                                Toast.makeText(this, "Error de base de datos : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        setLoadingState(false)
                        Toast.makeText(this, "Usuario no registrado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    setLoadingState(false)
                    Toast.makeText(this, "Error de autenticación: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                setLoadingState(true)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                setLoadingState(false)
                Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(acct?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión con Google exitoso
                    val user = auth.currentUser
                    val firestore = FirebaseFirestore.getInstance()

                    if (user != null) {
                        val userId = user.uid
                        val email = user.email
                        val nombres = acct?.givenName ?: ""
                        val apellidos = acct?.familyName ?: ""
                        val organizacion = ""  // Google no proporciona organización, pero se puede dejar en blanco

                        // Verificar si el usuario ya existe en Firestore
                        firestore.collection("users").document(userId).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // El usuario ya existe en Firestore, ahora verificar el deviceId
                                    val typeUser = document.getString("typeUser")
                                    saveUserDetailsInPreferences(userId,typeUser)
                                    val intent = Intent(this, TabOperationActivity::class.java)
                                    startActivity(intent)
                                    finish()

                                } else {
                                    // El usuario no existe, registrar la información en Firestore
                                    val userInfo = hashMapOf(
                                        "nombres" to nombres,
                                        "apellidos" to apellidos,
                                        "organizacion" to organizacion,
                                        "email" to email,
                                        "id" to userId,
                                        "typeLogin" to "Google",
                                        "typeUser" to "a"
                                    )

                                    // Guardar en Firestore
                                    firestore.collection("users").document(userId).set(userInfo)
                                        .addOnSuccessListener {
                                            saveUserDetailsInPreferences(userId,"a")
                                            val intent = Intent(this, TabOperationActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener {
                                            setLoadingState(false)
                                            Toast.makeText(this, "Error al registrar los datos en Firestore", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener {
                                setLoadingState(false)
                                Toast.makeText(this, "Error al verificar el usuario", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    setLoadingState(false)
                    Toast.makeText(this, "Error en la autenticación con Google", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun setLoadingState(isLoading: Boolean) {
        // Deshabilitar los campos y botón mientras se muestra el ProgressBar
        binding.emailEditText.isEnabled = !isLoading
        binding.passwordEditText.isEnabled = !isLoading
        binding.layoutLogin.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun saveUserDetailsInPreferences(userId: String, typeUser: String?) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("userId", userId)
            putString("typeUser", typeUser ?: "") // Guarda como cadena vacía si es null
            apply()
        }
    }

    private fun getUserDetailsFromPreferences(): Pair<String?, String?> {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", "")   // Retorna null si no existe la clave "userId"
        val typeUser = prefs.getString("typeUser", "") // Retorna null si no existe la clave "deviceId"
        return Pair(userId, typeUser)
    }

    override fun onStart() {
        super.onStart()
        val (userId) = getUserDetailsFromPreferences()
        if (userId != "") {
            startActivity(Intent(this, TabOperationActivity::class.java))
            finish()
        }
    }

    override fun onBackPressed() {
    }

}