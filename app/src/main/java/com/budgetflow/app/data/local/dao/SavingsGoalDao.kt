package com.budgetflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetflow.app.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY label")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE isActive = 1")
    suspend fun getAllActive(): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: SavingsGoalEntity): Long

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)
}
