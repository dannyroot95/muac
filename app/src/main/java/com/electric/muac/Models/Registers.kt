package com.electric.muac.Models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.sql.Timestamp

@Parcelize
@Entity(tableName = "registers")
data class Registers(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "fullname") val fullname: String,
    @ColumnInfo(name = "dni") val dni: String,
    @ColumnInfo(name = "age") val age: String,                  // Nuevo campo     // Nuevo campo
    @ColumnInfo(name = "history") val history: String,
    @ColumnInfo(name = "imc") val imc: Float,
    @ColumnInfo(name = "muac") val muac: Float,
    @ColumnInfo(name = "lat") val lat: Double,
    @ColumnInfo(name = "lng") val lng: Double,
    @ColumnInfo(name = "weight") val weight: Int,
    @ColumnInfo(name = "size") val size: Double,
    @ColumnInfo(name = "mode") var mode: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "idUser") val idUser: String,
    @ColumnInfo(name = "imcStatus") val imcStatus: String
) : Parcelable
