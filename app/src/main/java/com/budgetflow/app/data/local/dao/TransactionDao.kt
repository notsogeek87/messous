package com.budgetflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetflow.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeAllForProfile(profileId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE profileId = :profileId AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC"
    )
    fun observeInRangeForProfile(profileId: Long, startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionEntity>>

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE categoryId = :categoryId AND type = 'EXPENSE' AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay"
    )
    suspend fun sumExpensesForCategoryInRange(categoryId: Long, startEpochDay: Long, endEpochDay: Long): Double

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE profileId = :profileId AND categoryId = :categoryId AND type = 'EXPENSE' AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay"
    )
    suspend fun sumExpensesForCategoryInRangeForProfile(
        profileId: Long,
        categoryId: Long,
        startEpochDay: Long,
        endEpochDay: Long
    ): Double

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE accountId = :accountId AND type = 'INCOME'"
    )
    suspend fun sumIncomeForAccount(accountId: Long): Double

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE accountId = :accountId AND type = 'EXPENSE'"
    )
    suspend fun sumExpenseForAccount(accountId: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}
