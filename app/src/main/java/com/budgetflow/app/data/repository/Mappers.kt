package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.entity.AccountEntity
import com.budgetflow.app.data.local.entity.CategoryEntity
import com.budgetflow.app.data.local.entity.IncomeEntity
import com.budgetflow.app.data.local.entity.RecurringExpenseEntity
import com.budgetflow.app.data.local.entity.SavingsGoalEntity
import com.budgetflow.app.data.local.entity.TransactionEntity
import com.budgetflow.app.data.local.entity.VariableBudgetEntity
import com.budgetflow.app.domain.model.Account
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.Income
import com.budgetflow.app.domain.model.RecurringExpense
import com.budgetflow.app.domain.model.SavingsGoal
import com.budgetflow.app.domain.model.Transaction
import com.budgetflow.app.domain.model.TransactionType
import com.budgetflow.app.domain.model.VariableBudget
import com.budgetflow.engine.model.Frequency
import java.time.DayOfWeek
import java.time.LocalDate

// --- Account -----------------------------------------------------------------------------

fun AccountEntity.toDomain() = Account(id, name, initialBalance, currency, isArchived)
fun Account.toEntity(profileId: Long) = AccountEntity(id, name, initialBalance, currency, isArchived, profileId)

// --- Category ------------------------------------------------------------------------------

fun CategoryEntity.toDomain() = Category(id, name, group, icon, isDefault)
fun Category.toEntity(profileId: Long) = CategoryEntity(id, name, group, icon, isDefault, profileId)

// --- Income / RecurringExpense scheduling fields (shared shape) ------------------------------

fun IncomeEntity.toDomain() = Income(
    id = id,
    label = label,
    amount = amount,
    frequency = Frequency.valueOf(frequency),
    dayOfMonth = dayOfMonth,
    dayOfWeek = dayOfWeek?.let { DayOfWeek.of(it) },
    monthOfYear = monthOfYear,
    oneTimeDate = oneTimeDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    startDate = startDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    endDate = endDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    accountId = accountId,
    isActive = isActive
)

fun Income.toEntity(profileId: Long) = IncomeEntity(
    id = id,
    label = label,
    amount = amount,
    frequency = frequency.name,
    dayOfMonth = dayOfMonth,
    dayOfWeek = dayOfWeek?.value,
    monthOfYear = monthOfYear,
    oneTimeDateEpochDay = oneTimeDate?.toEpochDay(),
    startDateEpochDay = startDate?.toEpochDay(),
    endDateEpochDay = endDate?.toEpochDay(),
    accountId = accountId,
    isActive = isActive,
    profileId = profileId
)

fun RecurringExpenseEntity.toDomain() = RecurringExpense(
    id = id,
    label = label,
    amount = amount,
    frequency = Frequency.valueOf(frequency),
    dayOfMonth = dayOfMonth,
    dayOfWeek = dayOfWeek?.let { DayOfWeek.of(it) },
    monthOfYear = monthOfYear,
    oneTimeDate = oneTimeDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    startDate = startDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    endDate = endDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    accountId = accountId,
    categoryId = categoryId,
    isFixedAmount = isFixedAmount,
    isActive = isActive
)

fun RecurringExpense.toEntity(profileId: Long) = RecurringExpenseEntity(
    id = id,
    label = label,
    amount = amount,
    frequency = frequency.name,
    dayOfMonth = dayOfMonth,
    dayOfWeek = dayOfWeek?.value,
    monthOfYear = monthOfYear,
    oneTimeDateEpochDay = oneTimeDate?.toEpochDay(),
    startDateEpochDay = startDate?.toEpochDay(),
    endDateEpochDay = endDate?.toEpochDay(),
    accountId = accountId,
    categoryId = categoryId,
    isFixedAmount = isFixedAmount,
    isActive = isActive,
    profileId = profileId
)

// --- VariableBudget --------------------------------------------------------------------------

fun VariableBudgetEntity.toDomain() = VariableBudget(id, label, monthlyLimit, categoryId, isActive)
fun VariableBudget.toEntity(profileId: Long) = VariableBudgetEntity(id, label, monthlyLimit, categoryId, isActive, profileId)

// --- Transaction ---------------------------------------------------------------------------

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = amount,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    date = LocalDate.ofEpochDay(dateEpochDay),
    description = description,
    accountId = accountId,
    createdAtEpochMillis = createdAtEpochMillis
)

fun Transaction.toEntity(profileId: Long) = TransactionEntity(
    id = id,
    amount = amount,
    type = type.name,
    categoryId = categoryId,
    dateEpochDay = date.toEpochDay(),
    description = description,
    accountId = accountId,
    createdAtEpochMillis = createdAtEpochMillis,
    profileId = profileId
)

// --- SavingsGoal ----------------------------------------------------------------------------

fun SavingsGoalEntity.toDomain() = SavingsGoal(id, label, targetAmount, currentAmount, monthlyContribution, isActive)
fun SavingsGoal.toEntity(profileId: Long) =
    SavingsGoalEntity(id, label, targetAmount, currentAmount, monthlyContribution, isActive, profileId)
