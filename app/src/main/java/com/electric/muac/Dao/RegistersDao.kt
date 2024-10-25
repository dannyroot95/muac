package com.electric.muac.Dao

import androidx.room.*
import com.electric.muac.Models.Registers

@Dao
interface RegistersDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(register: Registers)

    @Query("SELECT * FROM registers WHERE dni = :dni ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastRegisterByDni(dni: String): Registers?

    @Query("SELECT * FROM registers WHERE mode = 'localy'")
    suspend fun getLocalRegisters(): List<Registers>

    @Query("SELECT * FROM registers")
    suspend fun getAllRegisters(): List<Registers>

    @Update
    suspend fun update(register: Registers)

    @Delete
    suspend fun deleteRegister(register: Registers)
}
