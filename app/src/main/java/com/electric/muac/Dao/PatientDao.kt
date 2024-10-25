package com.electric.muac.Dao

import androidx.room.*
import com.electric.muac.Models.Patient

@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: Patient)

    // Insertar múltiples pacientes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatients(patients: List<Patient>)

    @Query("SELECT * FROM patient WHERE dni = :dni")
    suspend fun getPatientByDni(dni: String): Patient?

    @Query("SELECT * FROM patient WHERE sex = :sex")
    suspend fun getPatientsBySex(sex: String): List<Patient>

    @Query("SELECT * FROM patient WHERE community = :community")
    suspend fun getPatientsByCommunity(community: String): List<Patient>

    @Query("SELECT * FROM patient")
    suspend fun getAllPatients(): List<Patient>

    @Delete
    suspend fun deletePatient(patient: Patient)

    // Eliminar todos los pacientes
    @Query("DELETE FROM patient")
    suspend fun deleteAllPatients()

}