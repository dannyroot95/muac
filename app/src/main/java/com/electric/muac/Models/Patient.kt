package com.electric.muac.Models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "patient")
data class Patient(
    @PrimaryKey @ColumnInfo(name = "dni") val dni: String = "",
    @ColumnInfo(name = "first_name") val firstName: String = "",
    @ColumnInfo(name = "last_name") val lastName: String = "",
    @ColumnInfo(name = "maternal_last_name") val maternalLastName: String = "",  // Nuevo campo
    @ColumnInfo(name = "sex") val sex: String = "",                               // Nuevo campo
    @ColumnInfo(name = "birthdate") val birthdate: String = "",                  // Nuevo campo
    @ColumnInfo(name = "community") val community: String = "",                  // Nuevo campo
    @ColumnInfo(name = "guardian_dni") val guardianDNI: String = "",             // Nuevo campo
    @ColumnInfo(name = "guardian_name") val guardianName: String = "",           // Nuevo campo
    @ColumnInfo(name = "history") val history: String = ""
): Parcelable {
    // Constructor vacío requerido por Firebase
    constructor() : this("", "", "", "", "", "", "", "", "", "")
}
