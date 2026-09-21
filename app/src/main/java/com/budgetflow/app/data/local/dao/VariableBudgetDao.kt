package com.budgetflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetflow.app.data.local.entity.VariableBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VariableBudgetDao {
    @Query("SELECT * FROM variable_budgets ORDER BY label")
    fun observeAll(): Flow<List<VariableBudgetEntity>>

    @Query("SELECT * FROM variable_budgets WHERE profileId = :profileId ORDER BY label")
    fun observeAllForProfile(profileId: Long): Flow<List<VariableBudgetEntity>>

    @Query("SELECT * FROM variable_budgets WHERE isActive = 1")
    suspend fun getAllActive(): List<VariableBudgetEntity>

    @Query("SELECT * FROM variable_budgets WHERE isActive = 1 AND profileId = :profileId")
    suspend fun getAllActiveForProfile(profileId: Long): List<VariableBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: VariableBudgetEntity): Long

    @Update
    suspend fun update(budget: VariableBudgetEntity)

    @Delete
    suspend fun delete(budget: VariableBudgetEntity)

    @Query("DELETE FROM variable_budgets WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}
