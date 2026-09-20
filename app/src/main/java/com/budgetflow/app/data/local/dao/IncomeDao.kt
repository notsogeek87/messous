package com.budgetflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetflow.app.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes ORDER BY label")
    fun observeAll(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE isActive = 1")
    suspend fun getAllActive(): List<IncomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(income: IncomeEntity): Long

    @Update
    suspend fun update(income: IncomeEntity)

    @Delete
    suspend fun delete(income: IncomeEntity)
}
